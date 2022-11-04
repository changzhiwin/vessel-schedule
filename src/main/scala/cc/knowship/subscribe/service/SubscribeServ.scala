package cc.knowship.subscribe.service

import java.util.UUID

import zio._
import zio.http.html._
import zio.http.model.{Headers}
import zio.http.{Client, Request, Body, URL}

import cc.knowship.subscribe.{AppConfig, SubscribeChange}
import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.table._
import cc.knowship.subscribe.service.WharfInformationServ

trait SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: String, infos: String): Task[Html] 

  def noitfy(change: SubscribeChange): Task[Unit]

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
  config: AppConfig
) extends SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: String, infos: String): Task[Html] = for {
    wharf        <- wharfTb.findByCode(wharfCode)
    wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code)).mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))
    selectedVoy  <- wharfInfServ.voyageOfVessel(vessel, voyage)
    twoRecord    <- voyageTb.findByShipNameAndOutVoy(vessel, selectedVoy).someOrElseZIO {
                      for {
                        twoBares  <- wharfInfServ.voyageStatus(vessel, selectedVoy).debug("Fetched")
                        vesselObj <- vesselTb.findOrCreate(twoBares._1, wharf.id)
                        voyageObj <- voyageTb.findOrCreate(twoBares._2, vesselObj.id)
                      } yield (vesselObj, voyageObj)
                    }
    subscription <- subscriptionTb.updateOrCreate(subscriberId, twoRecord._2.id, infos)
  } yield wharfInfServ.viewOfHtml(subscription, twoRecord._2, twoRecord._1, wharf)

  // 优化TODO: 可以按照voyage聚合之后，发通知；这样可以减少对vessel、voyage、wharf的查询
  def noitfy(change: SubscribeChange): Task[Unit]= for {
    subscriber   <- subscriberTb.get(change.subscription.subscriberId)
    voyage       <- voyageTb.get(change.subscription.voyageId)
    vessel       <- vesselTb.get(voyage.vesselId)
    wharf        <- wharfTb.get(vessel.wharfId)
    wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code)).mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))

    pushAPI      = URL.fromString(config.subscribe.notifyUrl)
    url          <- ZIO.fromEither(pushAPI).mapError(m => new URLParseFailed(s"$m"))

    headers      = Headers("X-Email-To", subscriber.openId) ++ Headers("X-Email-From", subscriber.receiver) ++ Headers("X-Email-Subject", change.title)
    body         = Body.fromString( wharfInfServ.viewOfHtml(change.subscription, voyage, vessel, wharf).encode.toString )
    response     <- client.request(Request.post(body, url).updateHeaders(h => h ++ headers))
    _            <- ZIO.log(s"Status of push response: ${response.status.text}")
    _            <- ZIO.whenCase(change) {
                      case SubscribeChange.Finish(_, subcpt) => mayDeleteLastSubscribeOnVoyage(subcpt.id, voyage.id, vessel.id)
                      case SubscribeChange.Update(_, subcpt) => subscriptionTb.update(subcpt.id)
                    }.when(response.status.isSuccess)
  } yield ()

  def cancel(subscriptionId: UUID): Task[Html]= for {
    subscription <- subscriptionTb.get(subscriptionId)   // TODO 一次查询拿到voyage、vessel、wharf
    voyage       <- voyageTb.get(subscription.voyageId)
    vessel       <- vesselTb.get(voyage.vesselId)
    wharf        <- wharfTb.get(vessel.wharfId)
    wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code)).mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))
    _            <- mayDeleteLastSubscribeOnVoyage(subscription.id, voyage.id, vessel.id)
  } yield wharfInfServ.viewOfHtml(subscription, voyage, vessel, wharf)

  private def mayDeleteLastSubscribeOnVoyage(subscriptionId: UUID, voyageId: UUID, vesselId: UUID): Task[Unit] = for {
    _ <- subscriptionTb.delete(subscriptionId)
    _ <- voyageTb.delete(voyageId).whenZIO(subscriptionTb.nonVoyageExist(voyageId))
    _ <- vesselTb.delete(vesselId).whenZIO(voyageTb.nonVesselExist(vesselId))
  } yield ()
}

object SubscribeServLive {
  val layer = ZLayer.fromFunction(SubscribeServLive.apply _)
}