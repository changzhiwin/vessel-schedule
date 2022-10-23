package cc.knowship.subscribe

import zio._
import zio.http._
import zio.http.middleware._

import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.table.{SubscriberTbLive, SubscriptionTbLive, VesselTbLive, VoyageTbLive, WharfTbLive}
import cc.knowship.subscribe.http._ //{SubscriberPartial, SubscriberPartialLive}
import cc.knowship.subscribe.service._
import cc.knowship.subscribe.scheduler._

object MainApp extends ZIOAppDefault {

  val httpApp = for {
    subscriber <- SubscriberPartial.routes
    subscribe  <- SubscribePartial.routes
    wharf      <- WharfPartial.routes
    _          <- FixScheduleStream.start(30.seconds)

    http = Http.collectZIO[Request] { subscribe.orElse(wharf).orElse(subscriber) }
    _ <- Server.serve(http.middleware(Middleware.patchZIO { response =>
           Console.printLine(s"status is error = ${response.status.isError}, headers = ${response.headers.toList.mkString("[", ",", "]")}").map(_ => Patch.empty).asSomeError
         }))
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

    // scheduler
    FixScheduleStream.layer,
    CheckExpiredPipeline.layer,
    FetchNewsPipeline.layer,
    NotifySink.layer,
  )
}