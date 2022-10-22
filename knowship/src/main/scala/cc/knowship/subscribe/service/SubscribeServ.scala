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

  def pushUpdate(subscription: Subscription): Task[Unit]

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
    twoRecord    <- voyageTb.findByShipNameAndOutVoy(vessel, selectedVoy).someOrElseZIO {
                      for {
                        twoBares  <- wharfInfServ.voyageStatus(vessel, selectedVoy)
                        vesselObj <- vesselTb.findOrCreate(twoBares._1, wharf.id).debug("vesselObj: ")
                        voyageObj <- voyageTb.findOrCreate(twoBares._2, vesselObj.id).debug("voyageObj: ")
                      } yield (vesselObj, voyageObj)
                    }

    // TODO，如果存在同一水，则不向远程请求，不更新voyage。
    //voyaStatus   <- wharfInfServ.voyageStatus(vessel, selectedVoy)
    //vesselObj    <- vesselTb.findOrCreate(voyaStatus._1, wharf.id).debug("vesselObj: ")
    //voyageObj    <- voyageTb.updateOrCreate(voyaStatus._2, vesselObj.id).debug("voyageObj: ")

    subscription <- subscriptionTb.updateOrCreate(subscriberId, twoRecord._2.id, infos).debug("subscription: ")
  } yield subscription

  def registeViewHtml(subscription: Subscription): Task[Html]= for {
    voyage <- voyageTb.get(subscription.voyageId)
    vessel <- vesselTb.get(voyage.vesselId)
  } yield {
    EmailTemplate.container(
      EmailTemplate.paragraph(s"Subcribe Success. ${vessel.shipName} / ${voyage.outVoy}, ${subscription.infos}")
    )
  }

  def pushUpdate(subscription: Subscription): Task[Unit]= for {
    // TODO 需要判断是不是最后一更新， 是的化需要删除和这个voyage相关的订阅
    _ <- Console.printLine(s"Publish subscription ${subscription.infos}, id = ${subscription.id}")
  } yield ()

  def cancel(subscriptionId: UUID): Task[Html]= for {
    subscription <- subscriptionTb.get(subscriptionId)
    voyage       <- voyageTb.get(subscription.voyageId)
    vessel       <- vesselTb.get(voyage.vesselId)
    _            <- mayDeleteLastSubscribeOnVoyage(voyage.id, vessel.id, subscriptionId)
  } yield {
    EmailTemplate.container(
      EmailTemplate.paragraph(s"Unsubcribe Success. ${vessel.shipName} / ${voyage.outVoy}, ${subscription.infos}")
    )
  }

  private def mayDeleteLastSubscribeOnVoyage(subscriptionId: UUID, voyageId: UUID, vesselId: UUID): Task[Unit] = for {
    _ <- subscriptionTb.delete(subscriptionId)
    _ <- voyageTb.delete(voyageId).whenZIO(subscriptionTb.nonVoyageExist(voyageId))
    _ <- vesselTb.delete(vesselId).whenZIO(voyageTb.nonVesselExist(vesselId))
  } yield ()
}

object SubscribeServLive {
  val layer = ZLayer.fromFunction(SubscribeServLive.apply _)
}