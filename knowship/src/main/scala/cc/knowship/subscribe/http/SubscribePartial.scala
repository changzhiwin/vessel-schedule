package cc.knowship.subscribe.http

import java.util.UUID

import zio._
import zio.http._
import zio.http.model.{Method}
import zio.json._

import cc.knowship.subscribe.service.{SubscriberServ, SubscribeServ}

trait SubscribePartial {
  def routes: PartialFunction[Request, Task[Response]]
}

object SubscribePartial {
  def routes: ZIO[SubscribePartial, Nothing, PartialFunction[Request, Task[Response]]] = 
    ZIO.serviceWith[SubscribePartial](_.routes)
}

case class SubscribePartialLive(subscriberServ: SubscriberServ, subscribeServ: SubscribeServ) extends SubscribePartial {

  override def routes = {

    case req @ Method.POST -> !! / "subscribe"  =>
      for {
        body         <- req.body.asString
        form         <- ZIO.fromEither(body.fromJson[SubscribeForm]).mapError(e => new Throwable(e))
        subscriber   <- subscriberServ.findOrCreate(form.openId, form.source, form.receiver, form.nicknameValue)
        subscription <- subscribeServ.registe(subscriber.id, form.wharfCode, form.vessel, form.voyage, form.infosValue)
        content      <- subscribeServ.registeViewHtml(subscription)
      } yield Response.html(content).updateHeaders(h => h ) // TODO ++ headers

    case Method.DELETE -> !! / "subscribe" / id  =>
      subscribeServ.cancel(UUID.fromString(id)).map(content => Response.html(content))
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
  def wharfCode: String = wharf.getOrElse("FAKE")
  def infosValue: String = infos.getOrElse("-")
}

object SubscribeForm {
  implicit val codec: JsonCodec[SubscribeForm] = DeriveJsonCodec.gen
}