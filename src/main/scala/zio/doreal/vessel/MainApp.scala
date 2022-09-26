package zio.doreal.vessel

import zio._
import zio.http._
import zio.http.model.{Method}
import zio.config._

import zio.doreal.vessel.dao.memory._
import zio.doreal.vessel.services._
import zio.doreal.vessel.wharf.cmg.CMGVesselService
import zio.doreal.vessel.controls.impl.EmailSubscribeVesselCtrl
import zio.doreal.vessel.controls.SubscribeVesselCtrl
object MainApp extends ZIOAppDefault {

  val httpRoute = Http.collectZIO[Request] {
    case Method.GET -> !! / "ping" => ZIO.succeed(Response.text("pong"))

    case req @ Method.GET -> !! / "schedule-status" => SubscribeVesselCtrl.handle(req)

    case req @ Method.POST -> !! / "notify-mock" => for {
      body <- req.body.asString
      _ <- ZIO.logInfo(req.toString)
    } yield Response.text("ok")
  }

  val myApp = for {
    config <- ZIO.service[AppConfig]
    _ <- ZIO.log(s"App start with ${config}")
    _ <- FileSourceService.init()
    scheduleFiber <- ScheduleFetchService.loop(config.fetchPeriod.minutes).repeat(Schedule.fixed(config.fetchPeriod.minutes)).fork
    fetchFiber <- FetchNewsService.start().fork
    publishFiber <- PublishSubscriptService.start().fork
    fileFiber <- FileSourceService.sync().repeat(Schedule.fixed(config.syncPeriod.minutes)).fork
    _ <- Server.serve(httpRoute)
    _ <- scheduleFiber.zip(fetchFiber).zip(publishFiber).zip(fileFiber).join
  } yield ()

  def run = myApp.provide(
    AppConfig.live,
    ServerConfig.live, //ServerConfig.live(ServerConfig.default.port(8090)), // TODO
    Client.default, 
    Scope.default, 
    Server.live, 
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

  val configApp = for {
    config <- ZIO.service[AppConfig]
    _ <- ZIO.log(s"${config}")
  } yield ()
}