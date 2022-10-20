package cc.knowship.subscribe.service

import java.util.UUID

import zio._
import zio.http.html._

import cc.knowship.subscribe.db.model.Subscription
import cc.knowship.subscribe.db.table._
import cc.knowship.subscribe.service.WharfInfoServ
import cc.knowship.subscribe.util.EmailTemplate

trait SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: Option[String], infos: String): Task[Subscription] 

  def registeViewHtml(subscription: Subscription): Task[Html]

  def cancel(subscriptionId: UUID): Task[Html]
}

case class SubscribeServLive(
  wharfTb:  WharfTb,
  vesselTb: VesselTb,
  voyageTb: VoyageTb,
  subscriptionTb: SubscriptionTb,
  wharfInfoServ: WharfInfoServ
) extends SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: Option[String], infos: String): Task[Subscription] = for {
    wharf          <- wharfTb.findByCode(wharfCode)
    // TODO, 根据wharf使用不同的服务
    selectedVoy    <- wharfInfoServ.voyageOfVessel(vessel, voyage)
    voyaStatus     <- wharfInfoServ.voyageStatus(vessel, selectedVoy)
    // TODO, 完全一样的提交，考虑findOrCreate
    vesselObj      <- vesselTb.create(voyaStatus._1, wharf.id)
    voyageObj      <- voyageTb.create(voyaStatus._2, vesselObj.id)
    subscription   <- subscriptionTb.create(subscriberId, voyageObj.id, infos)
  } yield subscription

  def registeViewHtml(subscription: Subscription): Task[Html]= for {
    voyage <- voyageTb.get(subscription.voyageId)
    vessel <- vesselTb.get(voyage.vesselId)
  } yield {
    EmailTemplate.container(
      EmailTemplate.paragraph(s"Subcribe Success. ${vessel.shipName} / ${voyage.outVoy}, ${subscription.infos}")
    )
  }

  def cancel(subscriptionId: UUID): Task[Html]= for {
    subscription <- subscriptionTb.get(subscriptionId)
    voyage       <- voyageTb.get(subscription.voyageId)
    vessel       <- vesselTb.get(voyage.vesselId)
    _            <- subscriptionTb.delete(subscriptionId)
    _            <- isNonSubscribeOnVoyage(voyage.id, vessel.id)
  } yield {
    EmailTemplate.container(
      EmailTemplate.paragraph(s"Unsubcribe Success. ${vessel.shipName} / ${voyage.outVoy}, ${subscription.infos}")
    )
  }

  private def isNonSubscribeOnVoyage(voyageId: UUID, vesselId: UUID): Task[Unit] = for {
    _ <- voyageTb.delete(voyageId).whenZIO(subscriptionTb.nonVoyageExist(voyageId))
    _ <- vesselTb.delete(vesselId).whenZIO(voyageTb.nonVesselExist(vesselId))
  } yield ()
}

object SubscribeServLive {
  val layer = ZLayer.fromFunction(SubscribeServLive.apply _)
}