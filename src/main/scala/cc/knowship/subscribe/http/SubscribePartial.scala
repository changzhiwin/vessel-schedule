package cc.knowship.subscribe.http

import java.util.UUID

import zio._
import zio.http._
import zio.http.model.{Method, Headers}
import zio.json._

import cc.knowship.subscribe.SubscribeException
import cc.knowship.subscribe.service.{SubscriberServ, SubscribeServ}

trait SubscribePartial {
  def routes: PartialFunction[Request, Task[Response]]
}

object SubscribePartial {
  def routes: ZIO[SubscribePartial, Nothing, PartialFunction[Request, Task[Response]]] = 
    ZIO.serviceWith[SubscribePartial](_.routes)
}

case class SubscribePartialLive(subscriberServ: SubscriberServ, subscribeServ: SubscribeServ) extends SubscribePartial {

  import SubscribeException._

  override def routes = {

    case req @ Method.POST -> !! / "subscribe"  =>
      for {
        body       <- req.body.asString
        form       <- ZIO.fromEither(body.fromJson[SubscribeForm]).mapError(_ => JsonDecodeFailed("SubscribeForm"))
        subscriber <- subscriberServ.findOrCreate(form.openId, form.source, form.receiver, form.nicknameValue)
        content    <- subscribeServ.registe(subscriber.id, form.wharfCode, form.vessel, form.voyageValue, form.infosValue)
      } yield Response.html(content._2).updateHeaders(h => {
        h ++
        Headers("X-Email-To", form.openId) ++ 
        Headers("X-Email-From", form.receiver) ++ 
        Headers("X-Email-Subject", "Subscribe: " + content._1)
      })

    case req @ Method.GET -> !! / "unsubscribe" / id  =>
      subscribeServ
        .cancel(UUID.fromString(id))
        .map(content => Response.html(content).updateHeaders(h => addHeaders(req, h, "Unsubscribe Success")))

    case req @ Method.POST -> !! / "mock-notify" =>
      ZIO.succeed(Response.text(req.headers.toList.mkString("[", ",", "]")))

    case req @ Method.GET -> !! / "welcome"  =>
      subscribeServ
        .welcome
        .map(content => Response.html(content).updateHeaders(h => addHeaders(req, h, "Welcome")))
  }

  private def addHeaders(req: Request, h: Headers, subject: String): Headers = {
    val from = req.url.queryParams.get("from").map(_.head).getOrElse("nobody@abc.com")
    val to = req.url.queryParams.get("to").map(_.head).getOrElse("error@abc.com")

    h ++
    Headers("X-Email-To", from) ++ 
    Headers("X-Email-From", to) ++ 
    Headers("X-Email-Subject", subject)
  }
}

object SubscribePartialLive {
  val layer = ZLayer.fromFunction(SubscribePartialLive.apply _)
}

final case class SubscribeForm(
  openId: String, 
  source: String, 
  receiver: String,
  nickname: Option[String],
  wharf: Option[String],
  vessel: String,
  voyage: Option[String],
  infos: Option[String]
) {
  def nicknameValue: String = {
    nickname match {
      case Some(nick) => nick
      case None       => {
        openId match {
          case s"${name}@${_}" => name
          case other: String   => other
        }
      }
    }
  }
  def wharfCode: String = wharf.getOrElse("SK").toUpperCase
  def infosValue: String = infos.getOrElse("-")
  def voyageValue: String = voyage.getOrElse("-")
}

object SubscribeForm {
  implicit val codec: JsonCodec[SubscribeForm] = DeriveJsonCodec.gen
}