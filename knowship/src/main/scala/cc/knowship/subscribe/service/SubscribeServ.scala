package cc.knowship.subscribe.service

import java.util.UUID

import zio._
import zio.http.html._

import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.model.Subscription
import cc.knowship.subscribe.db.table._
import cc.knowship.subscribe.service.WharfInformationServ
import cc.knowship.subscribe.util.EmailTemplate

trait SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: String, infos: String): Task[Subscription] 

  def registeViewHtml(subscription: Subscription): Task[Html]

  def cancel(subscriptionId: UUID): Task[Html]
}

case class SubscribeServLive(
  wharfTb:  WharfTb,
  vesselTb: VesselTb,
  voyageTb: VoyageTb,
  subscriptionTb: SubscriptionTb,
  wharfInformationServ: Map[String, WharfInformationServ]
) extends SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: String, infos: String): Task[Subscription] = for {
    wharf        <- wharfTb.findByCode(wharfCode)
    wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code))
                       .mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))

    selectedVoy  <- wharfInfServ.voyageOfVessel(vessel, voyage)
    voyaStatus   <- wharfInfServ.voyageStatus(vessel, selectedVoy)

    vesselObj    <- vesselTb.findOrCreate(voyaStatus._1, wharf.id).debug("vesselObj: ")
    voyageObj    <- voyageTb.updateOrCreate(voyaStatus._2, vesselObj.id).debug("voyageObj: ")
    subscription <- subscriptionTb.updateOrCreate(subscriberId, voyageObj.id, infos).debug("subscription: ")
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