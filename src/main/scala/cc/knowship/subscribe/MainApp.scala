package cc.knowship.subscribe

import zio._
import zio.http._
import zio.http.model._

import cc.knowship.subscribe.util.DebugUtils
import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.table.{SubscriberTbLive, SubscriptionTbLive, VesselTbLive, VoyageTbLive, WharfTbLive}
import cc.knowship.subscribe.http._
import cc.knowship.subscribe.service._
import cc.knowship.subscribe.scheduler._

object MainApp extends ZIOAppDefault {

  val httpApp = for {
    subscriber <- SubscriberPartial.routes
    subscribe  <- SubscribePartial.routes
    wharf      <- WharfPartial.routes
    _          <- FixScheduleStream.start(30.seconds)

    http = (Http.collectZIO[Request] { subscribe.orElse(wharf).orElse(subscriber) }).catchAll { error =>
      scala.Console.err.println(DebugUtils.prettify(error))
      Http.response( HttpError.InternalServerError(cause = Some(error)).toResponse ) @@ Middleware.beautifyErrors
    }
    _ <- Server.serve(http)
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