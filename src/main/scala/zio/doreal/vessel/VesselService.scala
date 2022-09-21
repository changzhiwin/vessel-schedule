package zio.doreal.vessel

import zio._
import zio.json._

case class ScheduleStatus(
  shipId: String,
  fullName: String,
  inVoya: String,
  outVoya: String,
  pob: String,
  etb: String,
  etd: String,
  inAgent: String,
  outAgent: String,
  notes: String
)

object ScheduleStatus {
  implicit val encodeScheduleStatus: JsonEncoder[ScheduleStatus] = DeriveJsonEncoder.gen[ScheduleStatus]
  implicit val decodeScheduleStatus: JsonDecoder[ScheduleStatus] = DeriveJsonDecoder.gen[ScheduleStatus]
}

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