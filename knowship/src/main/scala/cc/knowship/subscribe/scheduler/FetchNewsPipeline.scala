package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.model.{Voyage, Subscription}
import cc.knowship.subscribe.db.table.{VoyageTb, SubscriptionTb, VesselTb, WharfTb}
import cc.knowship.subscribe.service.WharfInformationServ

case class FetchNewsPipeline(
  voyageTb: VoyageTb, 
  subscriptionTb: SubscriptionTb,
  vesselTb: VesselTb, 
  wharfTb: WharfTb,
  wharfInformationServ: Map[String, WharfInformationServ]
) {

  def transform() = ZPipeline.mapZIO[Any, Throwable, Voyage, Chunk[Subscription]](

    voyage => for {
      vessel       <- vesselTb.get(voyage.vesselId)
      wharf        <- wharfTb.get(vessel.wharfId)
      wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code))
                        .mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))
      voyaStatus   <- wharfInfServ.voyageStatus(vessel.shipName, voyage.outVoy)
      notifySet    <- ZIO.ifZIO(voyageTb.hasNotifyAfterUpdate(voyage, voyaStatus._2))(
                        onTrue  = subscriptionTb.findByVoyage(voyage.id).map(Chunk.from(_)),
                        onFalse = ZIO.succeed(Chunk.empty[Subscription])
                      )
    } yield notifySet 
    
  ) >>> ZPipeline.mapChunks[Chunk[Subscription], Subscription](_.flatten)

}

object FetchNewsPipeline {
  val layer = ZLayer.fromFunction(FetchNewsPipeline.apply _)
}

