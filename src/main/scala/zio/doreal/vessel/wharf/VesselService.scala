package zio.doreal.vessel.wharf

import zio._
import zio.json._

import zio.doreal.vessel.entities.ScheduleStatus

case class ScheduleStatusReply(
  code: Int = 0,
  message: String = "Success",
  status: Option[ScheduleStatus] = None
)

object ScheduleStatusReply {
  implicit val encodeScheduleStatusReply: JsonEncoder[ScheduleStatusReply] = DeriveJsonEncoder.gen[ScheduleStatusReply]
  implicit val decodeScheduleStatusReply: JsonDecoder[ScheduleStatusReply] = DeriveJsonDecoder.gen[ScheduleStatusReply]
}

trait VesselService {

  def fetchScheduleStatus(vessel: String, voyage: String): Task[ScheduleStatusReply]
}