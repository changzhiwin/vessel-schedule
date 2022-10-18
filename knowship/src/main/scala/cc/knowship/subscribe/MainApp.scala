package cc.knowship.subscribe

import zio._
import zio.http._

import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.table.SubscriberTbLive
import cc.knowship.subscribe.http.{SubscriberPartial, SubscriberPartialLive}

object MainApp extends ZIOAppDefault {

  val httpApp = for {
    subscriber <- SubscriberPartial.routes

    http = Http.collectZIO[Request] { subscriber }
    _ <- Server.serve(http)
  } yield ()

  override def run = httpApp.provide(
    Server.default,
    QuillContext.dataSourceLayer,
    SubscriberTbLive.layer,
    SubscriberPartialLive.layer,
  )
}