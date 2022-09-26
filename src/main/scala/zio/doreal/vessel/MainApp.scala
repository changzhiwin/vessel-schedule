package zio.doreal.vessel

import zio._
import zio.http._
import zio.http.model.{Method}

import zio.doreal.vessel.dao.memory._
import zio.doreal.vessel.services._
//import zio.doreal.vessel.services.{SubscribeVesselServiceLive, ScheduleFetchService, ScheduleFetchServiceLive, FetchNewsService, PublishSubscriptService}
import zio.doreal.vessel.wharf.cmg.CMGVesselService
import zio.doreal.vessel.controls.impl.EmailSubscribeVesselCtrl
import zio.doreal.vessel.controls.SubscribeVesselCtrl

object MainApp extends ZIOAppDefault {

  val httpRoute = Http.collectZIO[Request] {
    case Method.GET -> !! / "ping" => ZIO.succeed(Response.text("pong"))

    case req @ Method.GET -> !! / "schedule-status" => SubscribeVesselCtrl.handle(req)

    case req @ Method.POST -> !! / "notify-mock" => for {
      body <- req.body.asString
      _ <- ZIO.logError(req.toString) *> ZIO.logError(body)
    } yield Response.text("ok")
  }

  val myApp = for {
    scheduleFiber <- ScheduleFetchService.loop().repeat(Schedule.fixed(30.seconds)).fork
    fetchFiber <- FetchNewsService.start().fork
    publishFiber <- PublishSubscriptService.start().fork
    fileFiber <- FileSourceService.sync().repeat(Schedule.fixed(60.seconds)).fork
    _ <- Server.serve(httpRoute)
    _ <- scheduleFiber.zip(fetchFiber).zip(publishFiber).zip(fileFiber).join
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
    ScheduleFetchServiceLive.live,
    FetchNewsService.live,
    PublishSubscriptService.live,
    FileSourceService.live
  ) //.exitCode
}