package zio.doreal.vessel.dao.memory

import zio._
//import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

import zio.doreal.vessel.entities.Shipment
import zio.doreal.vessel.dao.ShipmentDao

case class ShipmentDaoImpl(repo: Ref[List[Shipment]]) extends ShipmentDao {

  def save(queryKey: String, vessel: String, voyage: String, wharf: String, scheduleStatusId: String): Task[String] = for {
    id <- Random.nextUUID.map(_.toString())
    current <- Clock.currentTime(TimeUnit.MILLISECONDS)
    _ <- repo.update(_.appended(Shipment(id, current, current, current, queryKey, vessel, voyage, wharf, scheduleStatusId)))
  } yield id

  def findById(id: String): Task[Shipment] = for {
    table <- repo.get
    shipment <- table.find(row => row.id == id) match {
      case Some(s) => ZIO.succeed(s)
      case None    => ZIO.fail(new java.lang.Throwable(s"No such shipment with id [${id}]"))
    }
  } yield shipment

  def findByQueryKey(key: String): Task[Option[Shipment]] = for {
    table <- repo.get
  } yield table.find(row => row.queryKey == key)

  def delete(id: String): Task[Boolean] = for {
    _ <- repo.update(_.filter(row => (row.id != id)))
  } yield true

  def getAll(): Task[List[Shipment]] = repo.get

  def findExpiredUpdate(duration: Duration): Task[List[Shipment]] = for {
    table <- repo.get
    current <- Clock.currentTime(TimeUnit.MILLISECONDS)
  } yield table.filter(row => row.updateAt < (current - duration.toMillis()))

  def updateFetchTime(shipment: Shipment): Task[Boolean] = for {
    _ <- repo.update(_.filter(row => (row.id != shipment.id)))
    current <- Clock.currentTime(TimeUnit.MILLISECONDS)
    _ <- repo.update(_.appended(shipment.copy(updateAt = current)))
  } yield true

  def updateTimeAndDetail(shipment: Shipment, statusId: String): Task[Boolean] = for {
    _ <- repo.update(_.filter(row => (row.id != shipment.id)))
    current <- Clock.currentTime(TimeUnit.MILLISECONDS)
    _ <- repo.update(_.appended(shipment.copy(updateAt = current, notifyAt = current, scheduleStatusId = statusId)))
  } yield true

  def init(shipments: List[Shipment]): Task[Boolean] = repo.update(_ => shipments).map(_ => true)
}

object ShipmentDaoImpl {

  def live: ULayer[ShipmentDao] =
    ZLayer {
      Ref.make(List.empty[Shipment]).map(ShipmentDaoImpl.apply)
    }
}