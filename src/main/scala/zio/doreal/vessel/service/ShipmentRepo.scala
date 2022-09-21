package zio.doreal.vessel.service

import zio._
import zio.doreal.vessel.models.{Shipment}

trait ShipmentRepo {

  def register(vessel: String, voyage: String, wharf: String): Task[String]

  def findById(id: String): Task[Shipment]

  def getAll: Task[List[Shipment]]

  def delete(id: String): Task[Boolean]
}

object ShipmentRepo {

  def register(vessel: String, voyage: String, wharf: String): ZIO[ShipmentRepo, Throwable, String] =
    ZIO.serviceWithZIO[ShipmentRepo](_.register(vessel, voyage, wharf))

  def findById(id: String): ZIO[ShipmentRepo, Throwable, Shipment] =
    ZIO.serviceWithZIO[ShipmentRepo](_.findById(id))

  def getAll(): ZIO[ShipmentRepo, Throwable, List[Shipment]] =
    ZIO.serviceWithZIO[ShipmentRepo](_.getAll)

  def delete(id: String): ZIO[ShipmentRepo, Throwable, Boolean] =
    ZIO.serviceWithZIO[ShipmentRepo](_.delete(id))
}