package zio.doreal.vessel

import zio._
import zio.http._
import zio.http.model.{Method}

import zio.doreal.vessel.dao.memory._
import zio.doreal.vessel.services.SubscribeVesselServiceLive
import zio.doreal.vessel.wharf.cmg.CMGVesselService
import zio.doreal.vessel.controls.impl.EmailSubscribeVesselCtrl

import zio.doreal.vessel.controls.SubscribeVesselCtrl

object MainApp extends ZIOAppDefault {

  val httpApp = Http.collectZIO[Request] {
    case Method.GET -> !! / "ping" => ZIO.succeed(Response.text("pong"))

    case req @ Method.GET -> !! / "schedule-status" => SubscribeVesselCtrl.handle(req)
  }

  def run = {
    val config = ServerConfig.default.port(8090)
    val configLayer = ServerConfig.live(config)

    Server.serve(httpApp).provide(
        configLayer, Server.live, Client.default, Scope.default, 
        EmailSubscribeVesselCtrl.live,
        SubscribeVesselServiceLive.live,
        CMGVesselService.live,
        UserDaoImpl.live,
        SubscriptionDaoImpl.live,
        ShipmentDaoImpl.live,
        ScheduleStatusDaoImpl.live
    ).exitCode
  }
}