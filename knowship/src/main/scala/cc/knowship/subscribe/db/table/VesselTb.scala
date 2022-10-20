package cc.knowship.subscribe.db.table

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

import zio._
import io.getquill.context.qzio.ImplicitSyntax._

import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.model.Vessel

trait VesselTb {

  def create(vesselBare: Vessel, wharfId: UUID): Task[Vessel]

  def get(id: UUID): Task[Vessel]

  def delete(id: UUID): Task[Unit]
}

final case class VesselTbLive(
  dataSource: DataSource
) extends VesselTb {
  
  import QuillContext._
  implicit val env = Implicit(dataSource)

  def create(vesselBare: Vessel, wharfId: UUID): Task[Vessel] = for {
    id     <- Random.nextUUID
    now    <- Clock.currentTime(TimeUnit.MILLISECONDS)

    vessel = vesselBare.copy(id = id, wharfId = wharfId, createAt = now)
    _ <- run(query[Vessel].insertValue(lift(vessel))).implicitly
  } yield vessel

  def get(id: UUID): Task[Vessel] =
    run(
      query[Vessel]
        .filter(s => s.id == lift(id))
    )
    .map(_.headOption)
    .someOrFail(DbRecordNotFound(s"Vessel#id ${id}"))
    .implicitly

  def delete(id: UUID): Task[Unit] =
    run(
      query[Vessel]
        .filter(s => s.id == lift(id))
        .delete
    ).implicitly *> ZIO.unit

}

object VesselTbLive {

  val layer = ZLayer.fromFunction(VesselTbLive.apply _)
}