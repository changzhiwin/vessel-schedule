package cc.knowship.subscribe.http

import java.util.UUID

import zio._
import zio.http._
import zio.http.model.{Method}
import zio.json._

import cc.knowship.subscribe.db.table.SubscriberTb

trait SubscriberPartial {
  def routes: PartialFunction[Request, Task[Response]]
}

object SubscriberPartial {
  def routes: ZIO[SubscriberPartial, Nothing, PartialFunction[Request, Task[Response]]] = 
    ZIO.serviceWith[SubscriberPartial](_.routes)
}

case class SubscriberPartialLive(subscriberTb: SubscriberTb) extends SubscriberPartial {

  override def routes = {

    case Method.GET -> !! / "subscriber"       =>
      for {
        subscribers <- subscriberTb.all
      } yield Response.json(subscribers.toJson)

    case Method.GET -> !! / "subscriber" / id   =>
      for {
        subscriber <- subscriberTb.get(UUID.fromString(id))
      } yield Response.json(subscriber.toJson)

    case req @ Method.POST -> !! / "subscriber" =>
      for {
        body       <- req.body.asString
        form       <- ZIO.fromEither(body.fromJson[SubscriberForm].left.map(new Error(_)))
        subscriber <- subscriberTb.create(form.openId, form.source, form.receiver, form.nickname)
      } yield Response.json(subscriber.toJson)

    case Method.DELETE -> !! / "subscriber" / id   =>
      subscriberTb.delete(UUID.fromString(id)).map(_ => Response.ok)
  }
}

object SubscriberPartialLive {
  val layer = ZLayer.fromFunction(SubscriberPartialLive.apply _)
}

final case class SubscriberForm(
  openId: String, 
  source: String, 
  receiver: String,
) {

  def nickname: String = {
    openId match {
      case s"${name}@${_}" => name
      case other: String   => other
    }
  }
}

object SubscriberForm {
  implicit val codec: JsonCodec[SubscriberForm] = DeriveJsonCodec.gen
}