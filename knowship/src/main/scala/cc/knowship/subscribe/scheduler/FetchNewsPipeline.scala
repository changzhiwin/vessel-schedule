package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.model.{Voyage, Subscription}
import cc.knowship.subscribe.db.table.{VoyageTb, SubscriptionTb, VesselTb, WharfTb}

case class FetchNewsPipeline(
  voyageTb: VoyageTb, 
  subscriptionTb: SubscriptionTb,
  vesselTb: VesselTb, 
  wharfTb: WharfTb,
  wharfInformationServ: Map[String, WharfInformationServ]
) {

  def handler(period: Duration) = ZPipeline.mapZIO[Voyage, Chunk[Subscription]] { voyage =>
    
    for {
      vessel        <- vesselTb.get(voyage.vesselId)
      wharf         <- wharfTb.get(vessel.wharfId)
      wharfInfServ  <- ZIO.fromOption(wharfInformationServ.get(wharf.code))
                          .mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))
      voyaStatus    <- wharfInfServ.voyageStatus(vessel.shipName, voyage.outVoy)
      sub1 <- ZIO.ifZIO(voyageTb.shouldNotifyTheChange(voyage.id, voyaStatus._2))(
        onTrue  = subscriptionTb.findByVoyage(voyage.id).map(Chunk.from(_)),
        onFalse = ZIO.succeed(Chunk.empty[Subscription])
      )
      now     <- Clock.currentTime(TimeUnit.MILLISECONDS)
      sub2    <- subscriptionTb.findByExpiredPeriod(now - period.toMillis())
    } yield sub1 ++ sub2 // TODO: 去重
  } >>> ZPipeline.mapChunks[Chunk[Subscription], Subscription](_.flatten)

}

object FetchNewsPipeline {
  val layer = ZLayer.fromFunction(FetchNewsPipeline.apply _)
}

