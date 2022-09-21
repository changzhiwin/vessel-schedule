package zio.doreal.vessel

import zio._
import zio.json._

import zio.doreal.vessel.entities.ScheduleStatus

case class ResponseScheduleStatus(
  message: String = "Success",
  status: Option[ScheduleStatus] = None
)

object ResponseScheduleStatus {
  implicit val encodeResponseScheduleStatus: JsonEncoder[ResponseScheduleStatus] = DeriveJsonEncoder.gen[ResponseScheduleStatus]
  implicit val decodeResponseScheduleStatus: JsonDecoder[ResponseScheduleStatus] = DeriveJsonDecoder.gen[ResponseScheduleStatus]
}

trait VesselService {

  def fetchScheduleStatus(vessel: String, voyage: Option[String]): Task[ResponseScheduleStatus]
}

object VesselService {
  
  def fetchScheduleStatus(vessel: String, voyage: Option[String]): ZIO[VesselService, Throwable, ResponseScheduleStatus] =
    ZIO.serviceWithZIO[VesselService](_.fetchScheduleStatus(vessel, voyage))
}