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
import cc.knowship.subscribe.util.EmailTemplate
import cc.knowship.subscribe.db.model.{Wharf, Vessel, Voyage, Subscription}

trait SubscribeServ {

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: String, infos: String): Task[(String, Html)] 

  def noitfy(change: SubscribeChange): Task[Unit]

  def cancel(subscriptionId: UUID): Task[Html]

  def welcome: Task[Html]
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

  def registe(subscriberId: UUID, wharfCode: String, vessel: String, voyage: String, infos: String): Task[(String, Html)] = for {
    wharf        <- wharfTb.findByCode(wharfCode)
    wharfInfServ <- ZIO.fromOption(wharfInformationServ.get(wharf.code)).mapError(_ => WharfInfServNotFound(s"wharf code(${wharf.code})"))
    selectedVoy  <- wharfInfServ.voyageOfVessel(vessel, voyage)
    twoRecord    <- voyageTb.findByShipNameAndOutVoy(vessel, selectedVoy).someOrElseZIO {
                      ZIO.logSpan("registe") {
                        for {
                          twoBares  <- wharfInfServ.voyageStatus(vessel, selectedVoy).retry(Schedule.spaced(10.seconds) && Schedule.recurs(2))
                          _         <- ZIO.log(s"fetched, ${twoBares._1}, ${twoBares._2}")
                          vesselObj <- vesselTb.findOrCreate(twoBares._1, wharf.id)
                          voyageObj <- voyageTb.findOrCreate(twoBares._2, vesselObj.id)
                        } yield (vesselObj, voyageObj)
                      }
                    }
    subscription <- subscriptionTb.updateOrCreate(subscriberId, twoRecord._2.id, infos)
  } yield ( (s"[${twoRecord._1.shipName}/${twoRecord._2.outVoy}]@${subscription.id.toString}") -> wharfInfServ.viewOfHtml(subscription, twoRecord._2, twoRecord._1, wharf))

  // ??????TODO: ????????????voyage????????????????????????????????????????????????vessel???voyage???wharf?????????
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
    subscription <- subscriptionTb.get(subscriptionId)   // TODO ??????????????????voyage???vessel???wharf
    voyage       <- voyageTb.get(subscription.voyageId)
    vessel       <- vesselTb.get(voyage.vesselId)
    wharf        <- wharfTb.get(vessel.wharfId)
    _            <- mayDeleteLastSubscribeOnVoyage(subscription.id, voyage.id, vessel.id)
  } yield cacelViewOfHtml(subscription, voyage, vessel, wharf)

  def welcome: Task[Html]= {

    val extendInfos = Seq(
      "????????????" -> "= ?????? / ?????? / ???????????? / ?????? =",
      "????????????" -> "???????????????????????????=????????????/?????????",
      "????????????" -> "= DANUM 175 / 2244S / ?????? / NB =",
      "????????????" -> "NB-?????????SK-??????",
      "????????????" -> "??????(Re)????????????????????????????????????"
    )

    val body = EmailTemplate.container( 
      Seq( 
        EmailTemplate.paragraph("?????????????????????/??????????????????", Some(Seq("color" -> "firebrick"))),
        EmailTemplate.paragraph_hr,
        EmailTemplate.paragraph_2cols(extendInfos, Some(Seq("width" -> "20%")))
      )
    )

    ZIO.succeed(body)
  }

  private def mayDeleteLastSubscribeOnVoyage(subscriptionId: UUID, voyageId: UUID, vesselId: UUID): Task[Unit] = for {
    _ <- subscriptionTb.delete(subscriptionId)
    _ <- voyageTb.delete(voyageId).whenZIO(subscriptionTb.nonVoyageExist(voyageId))
    _ <- vesselTb.delete(vesselId).whenZIO(voyageTb.nonVesselExist(vesselId))
  } yield ()

  private def cacelViewOfHtml(subscription: Subscription, voyage: Voyage, vessel: Vessel, wharf: Wharf): Html = {

    val extendInfos = Seq(
      "????????????" -> "????????????",
    )

    val cancelInfos = Seq(
      "??????" -> wharf.name,
      "??????" -> vessel.shipName,
      "??????" -> voyage.outVoy,
      "??????" -> subscription.infos,
    )

    EmailTemplate.container( 
      Seq( 
        EmailTemplate.paragraph_2cols(extendInfos, None),
        EmailTemplate.paragraph_hr,
        EmailTemplate.paragraph_2cols(cancelInfos, None) 
      )
    )
  }
}

object SubscribeServLive {
  val layer = ZLayer.fromFunction(SubscribeServLive.apply _)
}