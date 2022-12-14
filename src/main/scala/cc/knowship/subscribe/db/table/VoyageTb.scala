package cc.knowship.subscribe.db.table

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

import zio._
import io.getquill.context.qzio.ImplicitSyntax._

import cc.knowship.subscribe.util.Constants
import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.model.{Voyage, Vessel} // Wharf

trait VoyageTb {

  def create(voyageBare: Voyage, vesselId: UUID): Task[Voyage]

  def update(voyage: Voyage): Task[Voyage]

  def findOrCreate(voyageBare: Voyage, vesselId: UUID): Task[Voyage]

  def get(id: UUID): Task[Voyage]

  def delete(id: UUID): Task[Unit]

  def nonVesselExist(vesselId: UUID): Task[Boolean]

  def findUpdateAtExpiredByWharf(wharfId: UUID, timestamp: Long): Task[List[Voyage]]

  //def hasNotifyAfterUpdate(voyageOld: Voyage, voyageBare: Voyage): Task[Boolean]

  def findByShipNameAndOutVoy(shipName: String, outVoy: String): Task[Option[(Vessel, Voyage)]]
}

object VoyageTb {

  def isChanged(vAged: Voyage, vNew: Voyage): Boolean = {
    if (vNew.atd != vAged.atd || vNew.etd != vAged.etd || vNew.etb != vAged.etb) true else false
  }

  def isFinished(vNew: Voyage): Boolean = vNew.atd != Constants.DEFAULT_STRING_VALUE
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

  def update(voyage: Voyage): Task[Voyage] = for {
    now       <- Clock.currentTime(TimeUnit.MILLISECONDS)

    voyageNew = voyage.copy(updateAt = now)
    _         <- run(query[Voyage].filter(_.id == lift(voyageNew.id)).updateValue(lift(voyageNew))).implicitly 
  } yield voyageNew

  def findOrCreate(voyageBare: Voyage, vesselId: UUID): Task[Voyage] = 
    run(query[Voyage].filter(s => s.outVoy == lift(voyageBare.outVoy) && s.vesselId == lift(vesselId)))
      .map(_.headOption)
      .implicitly
      .someOrElseZIO(self.create(voyageBare, vesselId))

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
    .map(_.isEmpty).implicitly

  def findUpdateAtExpiredByWharf(wharfId: UUID, timestamp: Long): Task[List[Voyage]] = {

    val q = quote {
      for {
        vessel <- query[Vessel] if (vessel.wharfId == lift(wharfId))
        voyage <- query[Voyage] if (voyage.vesselId == vessel.id && voyage.updateAt < lift(timestamp))
      } yield voyage
    }

    run(q).implicitly
  }

  def findByShipNameAndOutVoy(shipName: String, outVoy: String): Task[Option[(Vessel, Voyage)]] = {
    
    val q = quote {
      for {
        vessel <- query[Vessel] if (vessel.shipName == lift(shipName))
        voyage <- query[Voyage] if (voyage.vesselId == vessel.id && voyage.outVoy == lift(outVoy))
      } yield {
        (vessel, voyage)
      }
    }

    run(q).map(_.headOption).implicitly
  }

}

object VoyageTbLive {

  val layer = ZLayer.fromFunction(VoyageTbLive.apply _)
}