package zio.doreal.vessel

import zio._
//import zio.http._
//import zio.http.Server

import zhttp.http._
import zhttp.service.Server


object MainApp extends ZIOAppDefault {

  val app = Http.text("Hello, World!")

  def run = Server.start(8090, app)
}