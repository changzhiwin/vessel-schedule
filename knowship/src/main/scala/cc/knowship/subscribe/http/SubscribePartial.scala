package cc.knowship.subscribe.http

import java.util.UUID

import zio._
import zio.http._
import zio.http.model.{Method}
import zio.json._

import cc.knowship.subscribe.db.table._

trait SubscribePartial {
  def routes: PartialFunction[Request, Task[Response]]
}

object SubscribePartial {
  def routes: ZIO[SubscribePartial, Nothing, PartialFunction[Request, Task[Response]]] = 
    ZIO.serviceWith[SubscribePartial](_.routes)
}

case class SubscribePartialLive(subscriberTb: SubscriberTb) extends SubscribePartial {

  override def routes = {

    case req @ Method.POST -> !! / "subscribe"  =>
      for {
        body       <- req.body.asString
        form       <- ZIO.fromEither(body.fromJson[SubscribeForm].left.map(new Error(_)))
        subscriber <- subscriberTb.findByOpenId(form.openId, form.source)
        
      } yield Response.json(subscriber.toJson)

    case Method.DELETE -> !! / "subscribe" / id  =>
      //subscriberTb.delete(UUID.fromString(id)).map(_ => Response.ok)
  }
}

object SubscribePartialLive {
  val layer = ZLayer.fromFunction(SubscribePartialLive.apply _)
}

final case class SubscribeForm(
  openId: String, 
  source: String, 
  receiver: String,
)

object SubscribeForm {
  implicit val codec: JsonCodec[SubscribeForm] = DeriveJsonCodec.gen
}