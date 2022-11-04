package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

import cc.knowship.subscribe.SubscribeChange
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

  def transform() = 
    ZPipeline.mapZIO[Any, Throwable, Voyage, Chunk[SubscribeChange]](v => compareToRemoteInfo(v)) >>>
    ZPipeline.mapChunks[Chunk[SubscribeChange], SubscribeChange](_.flatten)

  private def compareToRemoteInfo(voyage: Voyage): Task[Chunk[SubscribeChange]] = for {
    vessel       <- vesselTb.get(voyage.vesselId)
    wharf        <- wharfTb.get(vessel.wharfId)
    wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code))
                      .mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))
    voyaStatus   <- wharfInfServ.voyageStatus(vessel.shipName, voyage.outVoy)

    voyageVo     = voyaStatus._2.copy(id = voyage.id, vesselId = voyage.vesselId, createAt = voyage.createAt)
    voyagePo     <- voyageTb.update(voyageVo)

    changedOpt   = wharfInfServ.hasChanged(voyage, voyagePo)
    finishedOpt  = wharfInfServ.hasFinished(voyagePo)
    subcptChunk  <- ZIO.ifZIO(ZIO.succeed(changedOpt.nonEmpty))(
                      onTrue  = subscriptionTb.findByVoyage(voyageVo.id).map(Chunk.from(_)),
                      onFalse = ZIO.succeed(Chunk.empty[Subscription])
                    )
    notifyChunk  <- ZIO.foreach(subcptChunk) { sub =>
                      finishedOpt match {
                        case Some(text) => ZIO.succeed(SubscribeChange.Finish(text, sub))
                        case None       => ZIO.succeed(SubscribeChange.Update(changedOpt.get, sub))
                      }
                    }
  } yield notifyChunk
}

object FetchNewsPipeline {
  val layer = ZLayer.fromFunction(FetchNewsPipeline.apply _)
}

