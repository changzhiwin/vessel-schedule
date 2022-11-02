package cc.knowship.subscribe.http

import java.util.UUID

import zio._
import zio.http._
import zio.http.model.{Method}
import zio.json._

import cc.knowship.subscribe.util.Constants
import cc.knowship.subscribe.db.model.Wharf
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
        wharf <- wharfTb.create(buildModel(form))
      } yield Response.json(wharf.toJson)

    case Method.DELETE -> !! / "wharf" / id   =>
      wharfTb.delete(UUID.fromString(id)).map(_ => Response.ok)
  }

  private def buildModel(form: WharfForm): Wharf = Wharf(
    id = Constants.DEFAULT_UUID,
    name = form.name,
    code = form.code,
    website = form.website.getOrElse("-"),
    period = form.period.getOrElse(900000),         // 默认15分钟 = 15 * 60 * 1000
    workStart = form.workStart.getOrElse(21600000), // 默认，早晨6点 = 6 * 60 * 60 * 1000
    workEnd = form.workEnd.getOrElse(84600000),      // 默认，晚上23:30 = (23 * 60 + 30) * 60 * 1000
    createAt = Constants.DEFAULT_EPOCH_MILLI
  )
}

object WharfPartialLive {
  val layer = ZLayer.fromFunction(WharfPartialLive.apply _)
}

final case class WharfForm(
  name: String, 
  code: String, 
  website: Option[String],
  period: Option[Long],
  workStart: Option[Long],
  workEnd: Option[Long]
)

object WharfForm {
  implicit val codec: JsonCodec[WharfForm] = DeriveJsonCodec.gen
}