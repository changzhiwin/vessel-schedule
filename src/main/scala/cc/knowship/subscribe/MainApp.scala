package cc.knowship.subscribe

import java.nio.file.{Paths}
import java.time.format.DateTimeFormatter

import zio._
import zio.http._
import zio.http.model._
import zio.logging.{ LogFormat, file }

import cc.knowship.subscribe.util.{DebugUtils, EmailTemplate, TimeDateUtils}
import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.table.{SubscriberTbLive, SubscriptionTbLive, VesselTbLive, VoyageTbLive, WharfTbLive}
import cc.knowship.subscribe.http._
import cc.knowship.subscribe.service._
import cc.knowship.subscribe.scheduler._

object MainApp extends ZIOAppDefault {

  val httpApp = for {
    config     <- ZIO.service[AppConfig]
    subscriber <- SubscriberPartial.routes
    subscribe  <- SubscribePartial.routes
    wharf      <- WharfPartial.routes
    _          <- FixScheduleStream.start(config.subscribe.loopUnit)

    http = (Http.collectZIO[Request] { subscribe.orElse(wharf).orElse(subscriber) }).catchAll { error =>
      scala.Console.err.println(DebugUtils.prettify(error))
      config.subscribe.env match {
        case "dev" => Http.response( HttpError.InternalServerError(cause = Some(error)).toResponse ) @@ Middleware.beautifyErrors
        case _     => Http.html(EmailTemplate.errorEmail(error.getMessage)).updateHeaders(h => {
          h ++ Headers("X-Email-To", "changzhiwin@qq.com") ++ Headers("X-Email-Subject", "System Error")
        })
      }
    }
    port <- Server.install(http)
    _    <- ZIO.log(s"App runnting in ${config.subscribe.env} mode, bind at ${port}.")
    _    <- ZIO.never
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

    // config
    AppConfig.layer,
  )

  import LogFormat._

  private val logformat =
    bracketStart |-| 
      timestamp(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).fixed(19) |-| 
      level  |-| 
    bracketEnd |-| 
    line |-| 
    //annotation(LogAnnotation.TraceId) |-| 
    spans |-| 
    cause

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> file(
      destination = Paths.get(s"./logs/app_${TimeDateUtils.currentLocalDateStr}.log"), 
      format = logformat,
      logLevel = LogLevel.Info
    )
}