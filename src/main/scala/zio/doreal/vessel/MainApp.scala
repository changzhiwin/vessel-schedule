package zio.doreal.vessel

import zio._
import zio.http._
import zio.http.model.{Method}

import zio.doreal.vessel.dao.memory._
import zio.doreal.vessel.services.{SubscribeVesselServiceLive, SchedulePullService, SchedulePullServiceLive, PullNewsService}
import zio.doreal.vessel.wharf.cmg.CMGVesselService
import zio.doreal.vessel.controls.impl.EmailSubscribeVesselCtrl
import zio.doreal.vessel.controls.SubscribeVesselCtrl

object MainApp extends ZIOAppDefault {

  val httpRoute = Http.collectZIO[Request] {
    case Method.GET -> !! / "ping" => ZIO.succeed(Response.text("pong"))

    case req @ Method.GET -> !! / "schedule-status" => SubscribeVesselCtrl.handle(req)
  }

  val myApp = for {
    scheduleFiber <- SchedulePullService.loop().repeat(Schedule.fixed(10.seconds)).fork
    pullFiber <- PullNewsService.start().fork
    _ <- Server.serve(httpRoute)
    _ <- scheduleFiber.zip(pullFiber).join
  } yield ()

  def run = myApp.provide(
    ServerConfig.live(ServerConfig.default.port(8090)),
    Server.live, 
    Client.default, 
    Scope.default, 
    EmailSubscribeVesselCtrl.live,
    SubscribeVesselServiceLive.live,
    CMGVesselService.live,
    UserDaoImpl.live,
    SubscriptionDaoImpl.live,
    ShipmentDaoImpl.live,
    ScheduleStatusDaoImpl.live,
    SchedulePullServiceLive.live,
    PullNewsService.live
  ) //.exitCode
}