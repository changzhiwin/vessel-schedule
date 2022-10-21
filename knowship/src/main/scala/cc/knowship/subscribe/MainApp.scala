package cc.knowship.subscribe

import zio._
import zio.http._
import zio.http.middleware._

import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.table.{SubscriberTbLive, SubscriptionTbLive, VesselTbLive, VoyageTbLive, WharfTbLive}
import cc.knowship.subscribe.http._ //{SubscriberPartial, SubscriberPartialLive}
import cc.knowship.subscribe.service._
//import cc.knowship.subscribe.wharf.FakeWharfInformation

object MainApp extends ZIOAppDefault {

  val httpApp = for {
    subscriber <- SubscriberPartial.routes
    subscribe  <- SubscribePartial.routes
    wharf      <- WharfPartial.routes

    http = Http.collectZIO[Request] { subscribe.orElse(wharf).orElse(subscriber) }
    _ <- Server.serve(http @@ Middleware.beautifyErrors)
  } yield ()

  override def run = httpApp.provide(
    // http
    Scope.default,
    Client.default,
    Server.default,
    SubscriberPartialLive.layer,
    SubscribePartialLive.layer,
    WharfPartialLive.layer,

    // db
    QuillContext.dataSourceLayer,
    SubscriberTbLive.layer,
    SubscriptionTbLive.layer,
    VesselTbLive.layer,
    WharfTbLive.layer,
    VoyageTbLive.layer,

    // service
    SubscribeServLive.layer,
    SubscriberServLive.layer,
    WharfInformationServ.layer,
  )
}