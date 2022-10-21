package cc.knowship.subscribe.db.table

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

import zio._
import io.getquill.context.qzio.ImplicitSyntax._

import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.model.Voyage

trait VoyageTb {

  def create(voyageBare: Voyage, vesselId: UUID): Task[Voyage]

  def updateOrCreate(voyageBare: Voyage, vesselId: UUID): Task[Voyage]

  def get(id: UUID): Task[Voyage]

  def delete(id: UUID): Task[Unit]

  def nonVesselExist(vesselId: UUID): Task[Boolean]
}

final case class VoyageTbLive(
  dataSource: DataSource
) extends VoyageTb { self =>
  
  import QuillContext._
  implicit val env = Implicit(dataSource)

  def create(voyageBare: Voyage, vesselId: UUID): Task[Voyage] = for {
    id     <- Random.nextUUID
    now    <- Clock.currentTime(TimeUnit.MILLISECONDS)

    voyage = voyageBare.copy(id = id, vesselId = vesselId, createAt = now, updateAt = now)
    _ <- run(query[Voyage].insertValue(lift(voyage))).implicitly
  } yield voyage

  def updateOrCreate(voyageBare: Voyage, vesselId: UUID): Task[Voyage] = for {
    recordOpt <- run(query[Voyage].filter(s => s.outVoy == lift(voyageBare.outVoy) && s.vesselId == lift(vesselId))).map(_.headOption).implicitly
    now       <- Clock.currentTime(TimeUnit.MILLISECONDS)
    voyage    <- ZIO.fromOption(recordOpt).foldZIO(
      notFound => self.create(voyageBare, vesselId),
      valFound => {
        val voyageObj = voyageBare.copy(id = valFound.id, vesselId = vesselId, createAt = valFound.createAt, updateAt = now)
        run(
          query[Voyage]
            .filter(_.id == lift(valFound.id))
            .updateValue(lift(voyageObj))
        ).implicitly *> ZIO.succeed(voyageObj)
      }
    )
  } yield voyage

  def get(id: UUID): Task[Voyage] =
    run(
      query[Voyage]
        .filter(s => s.id == lift(id))
    )
    .map(_.headOption)
    .someOrFail(DbRecordNotFound(s"Voyage#id ${id.toString}"))
    .implicitly

  def delete(id: UUID): Task[Unit] = 
    run(
      query[Voyage]
        .filter(s => s.id == lift(id))
        .delete
    ).implicitly *> ZIO.unit

  def nonVesselExist(vesselId: UUID): Task[Boolean] =
    run(
      query[Voyage].filter(s => s.vesselId == lift(vesselId)) 
    )
    .map(_.isEmpty)
    .implicitly

}

object VoyageTbLive {

  val layer = ZLayer.fromFunction(VoyageTbLive.apply _)
}