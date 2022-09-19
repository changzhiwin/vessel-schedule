package zio.doreal.vessel

import zio.http._
import zio.http.model.Method
import zio.{Scope, ZIO, ZIOAppDefault}

object ClientServer extends ZIOAppDefault {

  val app = Http.collectZIO[Request] {
    case Method.GET -> !! / "hello" =>
      ZIO.succeed(Response.text("hello"))

    case Method.GET -> !! =>
      val url = "http://eportapisct.scctcn.com/api/GVesselVoyage?VesselName=XIN%20TIAN%20JIN&PageIndex=1&PageSize=999"
      Client.request(url)

    case Method.GET -> !! / "vessel" / name => for {
      _    <- ZIO.succeed(name).debug("Get param:")

      url  <- URL.fromString("http://eportapisct.scctcn.com/api/GVesselVoyage") match {
        case Left(l)  => ZIO.fail(l)
        case Right(r) => ZIO.succeed(r)
      }
      response <- Client.request(Request(url = url.setQueryParams(s"VesselName=${name}&PageIndex=1&PageSize=999") ))
      body <- response.body.asString.debug("body string: ")
    } yield Response.json(body)
  }

  val run = {
    Server.serve(app).provide(Server.default, Client.default, Scope.default).exitCode
  }
}
