package zio.doreal.vessel.dao

//import scala.concurrent.duration.FiniteDuration

import zio._
import zio.doreal.vessel.entities.{Shipment}

trait ShipmentDao {

  def save(queryKey: String, vessel: String, voyage: String, wharf: String, scheduleStatusId: String): Task[String]

  def findById(id: String): Task[Shipment]

  def findByQueryKey(key: String): Task[Option[Shipment]]

  def delete(id: String): Task[Boolean]

  def getAll(): Task[List[Shipment]]

  def findExpiredUpdate(duration: Duration): Task[List[Shipment]]

  def updateFetchTime(shipment: Shipment): Task[Boolean]

  def updateTimeAndDetail(shipment: Shipment, statusId: String): Task[Boolean]
}