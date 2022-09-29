package zio.doreal.vessel.controls.impl

import zio._
import zio.http.{Request, Response}
import zio.http.model.{Headers}

import zio.doreal.vessel.controls.vo.SubscribeParams
import zio.doreal.vessel.controls.SubscribeVesselCtrl
import zio.doreal.vessel.services.SubscribeVesselService

case class EmailSubscribeVesselCtrl(subscribeVesselService: SubscribeVesselService) extends SubscribeVesselCtrl {

  override def handle(req: Request): ZIO[Any, Throwable, Response] = {

    val subParams = SubscribeParams.fromEmailChannel(req.url.queryParams)
    val headers = Headers("X-Email-To", subParams.from) ++ 
      Headers("X-Email-Bcc", "bebest@88.com") ++
      Headers("X-Email-From", subParams.to) ++ 
      Headers("X-Email-Subject", "Y")

    for {
      _       <- ZIO.log("Request: " + subParams.debugPrint)
      content <- subParams.action match {
        case "SUBSCRIBE"   => subscribeVesselService.subscribe(subParams)
        case "UNSUBSCRIBE" => subscribeVesselService.unsubscribe(subParams)
        case "ADMIN"       => subscribeVesselService.admin(subParams)
        case _             => ZIO.succeed("Not support.")
      }
    } yield Response.text(content).updateHeaders(h => h ++ headers)
  }
}

object EmailSubscribeVesselCtrl {

  val live: ZLayer[SubscribeVesselService, Nothing, SubscribeVesselCtrl] = ZLayer {
    for {
      subscribeVesselService <- ZIO.service[SubscribeVesselService]
    } yield EmailSubscribeVesselCtrl(subscribeVesselService)
  }
}
