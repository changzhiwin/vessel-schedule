package zio.doreal.vessel

import zio.http._
import zio.json._
import zio.http.model.{Method, Headers, HttpError}
import zio.{Scope, ZIO, ZIOAppDefault}

import zio.doreal.vessel.{VesselService, ScheduleStatus}
import zio.doreal.vessel.cmg.CMGVesselService


object MainApp extends ZIOAppDefault {

  val app = Http.collectZIO[Request] {
    case Method.GET -> !! / "hello" =>
      ZIO.succeed(Response.text("hello"))

    case req @ Method.GET -> !! / "schedule-status" => for {
      _ <- ZIO.unit

      queryParams = req.url.queryParams
      vesselOpt = queryParams.get("vessel").map(_.head)
      voyageOpt = queryParams.get("voyage").map(_.head)
      fromOpt = queryParams.get("from").map(_.head)
      toOpt   = queryParams.get("to").map(_.head)
      headers = Headers("X-Email-To", fromOpt.get) ++ Headers("X-Email-From", toOpt.get) ++ Headers("X-Email-Subject", "Y")
      
      _ <- ZIO.log(s"headers = ${headers}")
      responseStr <- (vesselOpt match {
        case None => ZIO.succeed("""{"message":"Params missing: [vessel]"}""")
        case Some(vessel)         => VesselService
          .fetchScheduleStatus(vessel, voyageOpt)
          .map(_.toJsonPretty)
      })//.debug("Response: ")
    } yield Response.json(responseStr).updateHeaders(h => h ++ headers)
  }

  val appWithErrorHanding = app.catchAll(e => Http.response( Response.fromHttpError(HttpError.Custom(-1, e.getMessage)) ))

  val run = {
    val config = ServerConfig.default.port(8090)
    val configLayer = ServerConfig.live(config)

    Server.serve(appWithErrorHanding).provide(configLayer, Server.live, Client.default, Scope.default, CMGVesselService.live).exitCode
  }
}