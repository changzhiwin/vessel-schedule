package zio.doreal.vessel.service

import zio._
import zio.doreal.vessel.models.{ShipmentStatus}

trait ShipmentStatusRepo {

  def register(shipmentId: String, statusId: String): ZIO[Any, Throwable, Boolean]

  def findByShipmentId(id: String): ZIO[Any, Throwable, String]

  def deleteByShipmentId(id: String): ZIO[Any, Throwable, Boolean]
}

object ShipmentStatusRepo {

  def register(shipmentId: String, statusId: String): ZIO[ShipmentStatusRepo, Throwable, Boolean] =
    ZIO.serviceWithZIO[ShipmentStatusRepo](_.register(shipmentId, statusId))

  def findByShipmentId(id: String): ZIO[ShipmentStatusRepo, Throwable, String] =
    ZIO.serviceWithZIO[ShipmentStatusRepo](_.findByShipmentId(id))

  def deleteByShipmentId(id: String): ZIO[ShipmentStatusRepo, Throwable, Boolean] =
    ZIO.serviceWithZIO[ShipmentStatusRepo](_.deleteByShipmentId(id))

}