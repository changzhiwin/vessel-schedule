package cc.knowship.subscribe.service

import java.util.UUID

import zio._
import zio.http.html._
import zio.http.model.{Headers}
import zio.http.{Client, Request, Body, URL}

import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.model.{Subscription, Voyage, Vessel}
import cc.knowship.subscribe.db.table._
import cc.knowship.subscribe.service.WharfInformationServ
import cc.knowship.subscribe.util.EmailTemplate

trait SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: String, infos: String): Task[Html] 

  def pushUpdate(subscription: Subscription): Task[Unit]

  def cancel(subscriptionId: UUID): Task[Html]
}

case class SubscribeServLive(
  wharfTb:  WharfTb,
  vesselTb: VesselTb,
  voyageTb: VoyageTb,
  subscriberTb: SubscriberTb,
  subscriptionTb: SubscriptionTb,
  wharfInformationServ: Map[String, WharfInformationServ],
  client: Client,
) extends SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: String, infos: String): Task[Html] = for {
    wharf        <- wharfTb.findByCode(wharfCode)
    wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code)).mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))
    selectedVoy  <- wharfInfServ.voyageOfVessel(vessel, voyage)
    twoRecord    <- voyageTb.findByShipNameAndOutVoy(vessel, selectedVoy).someOrElseZIO {
                      for {
                        twoBares  <- wharfInfServ.voyageStatus(vessel, selectedVoy).debug("Fetched")
                        vesselObj <- vesselTb.findOrCreate(twoBares._1, wharf.id).debug("vesselObj")
                        voyageObj <- voyageTb.findOrCreate(twoBares._2, vesselObj.id).debug("voyageObj")
                      } yield (vesselObj, voyageObj)
                    }
    subscription <- subscriptionTb.updateOrCreate(subscriberId, twoRecord._2.id, infos)
  } yield viewOfHtml(SubscribeState.Updating("Registe"), subscription, twoRecord._2, twoRecord._1)

  // 优化TODO: 可以按照voyage聚合之后，发通知；这样可以减少对vessel、voyage、wharf的查询
  def pushUpdate(subscription: Subscription): Task[Unit]= for {
    subscriber   <- subscriberTb.get(subscription.subscriberId)
    voyage       <- voyageTb.get(subscription.voyageId)
    vessel       <- vesselTb.get(voyage.vesselId)
    wharf        <- wharfTb.get(vessel.wharfId)
    wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code)).mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))

    pushAPI      = URL.fromString("http://127.0.0.1:8080/mock-push-notify")
    url          <- ZIO.fromEither(pushAPI).mapError(m => new URLParseFailed(s"$m"))

    isFinish     = wharfInfServ.isFinished(voyage)
    state        = if (isFinish) SubscribeState.Finished("Finish") else SubscribeState.Updating("Update")
    headers      = Headers("X-Email-To", subscriber.openId) ++ Headers("X-Email-From", subscriber.receiver) ++ Headers("X-Email-Subject", state.detail)
    body         = Body.fromString(viewOfHtml(state, subscription, voyage, vessel).encode.toString)
    response     <- client.request(Request.post(body, url).updateHeaders(h => h ++ headers))
    _            <- response.body.asString.debug("Push response body")
    _            <- ZIO.ifZIO(ZIO.succeed(isFinish))(
                      onTrue  = mayDeleteLastSubscribeOnVoyage(subscription.id, voyage.id, vessel.id),
                      onFalse = subscriptionTb.update(subscription.id)
                    ).debug("After push")
  } yield ()

  def cancel(subscriptionId: UUID): Task[Html]= for {
    subscription <- subscriptionTb.get(subscriptionId)
    voyage       <- voyageTb.get(subscription.voyageId)
    vessel       <- vesselTb.get(voyage.vesselId)
    _            <- mayDeleteLastSubscribeOnVoyage(subscription.id, voyage.id, vessel.id)
  } yield viewOfHtml(SubscribeState.Canceled("Cancel"), subscription, voyage, vessel)

  private def mayDeleteLastSubscribeOnVoyage(subscriptionId: UUID, voyageId: UUID, vesselId: UUID): Task[Unit] = for {
    _ <- subscriptionTb.delete(subscriptionId)
    _ <- voyageTb.delete(voyageId).whenZIO(subscriptionTb.nonVoyageExist(voyageId))
    _ <- vesselTb.delete(vesselId).whenZIO(voyageTb.nonVesselExist(vesselId))
  } yield ()

  private def viewOfHtml(state: SubscribeState, subscription: Subscription, voyage: Voyage, vessel: Vessel): Html = {
    EmailTemplate.container(
      Seq(
        EmailTemplate.paragraph(s"State: ${state.detail}"),
        EmailTemplate.paragraph(s"Subcribe Success. ${vessel.shipName} / ${voyage.outVoy} / ${subscription.infos}")
      )
    )
  }
}

object SubscribeServLive {
  val layer = ZLayer.fromFunction(SubscribeServLive.apply _)
}

trait SubscribeState {
  def detail: String
}

object SubscribeState {

  case class Updating(detail: String) extends SubscribeState
  case class Finished(detail: String) extends SubscribeState
  case class Canceled(detail: String) extends SubscribeState
}