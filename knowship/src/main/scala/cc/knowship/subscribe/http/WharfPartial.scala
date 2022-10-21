package cc.knowship.subscribe.http

import java.util.UUID

import zio._
import zio.http._
import zio.http.model.{Method}
import zio.json._

import cc.knowship.subscribe.db.table.WharfTb

trait WharfPartial {
  def routes: PartialFunction[Request, Task[Response]]
}

object WharfPartial {
  def routes: ZIO[WharfPartial, Nothing, PartialFunction[Request, Task[Response]]] = 
    ZIO.serviceWith[WharfPartial](_.routes)
}

case class WharfPartialLive(wharfTb: WharfTb) extends WharfPartial {

  override def routes = {

    case Method.GET -> !! / "wharf"       =>
      for {
        wharfs <- wharfTb.all
      } yield Response.json(wharfs.toJson)

    case Method.GET -> !! / "wharf" / id   =>
      for {
        wharf <- wharfTb.get(UUID.fromString(id))
      } yield Response.json(wharf.toJson)

    case req @ Method.POST -> !! / "wharf" =>
      for {
        body  <- req.body.asString
        form  <- ZIO.fromEither(body.fromJson[WharfForm].left.map(new Error(_)))
        wharf <- wharfTb.create(form.name, form.code, form.website.getOrElse("-"))
      } yield Response.json(wharf.toJson)

    case Method.DELETE -> !! / "wharf" / id   =>
      wharfTb.delete(UUID.fromString(id)).map(_ => Response.ok)
  }
}

object WharfPartialLive {
  val layer = ZLayer.fromFunction(WharfPartialLive.apply _)
}

final case class WharfForm(
  name: String, 
  code: String, 
  website: Option[String]
)

object WharfForm {
  implicit val codec: JsonCodec[WharfForm] = DeriveJsonCodec.gen
}