package zio.doreal.vessel

import zio._
import zio.http.Client

object SimpleClient {
  val url = "http://eportapisct.scctcn.com/api/GVesselVoyage?VesselName=XIN%20TIAN%20JIN&PageIndex=1&PageSize=999"

  val program = for {
    res  <- Client.request(url)
    data <- res.body.asString
    _    <- Console.printLine(data)
  } yield ()

  //override val run = program.provide(Client.default, Scope.default)

}
