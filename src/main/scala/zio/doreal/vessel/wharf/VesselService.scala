package zio.doreal.vessel.wharf

import zio._
import zio.json._

import zio.doreal.vessel.entities.ScheduleStatus
import zio.doreal.vessel.wharf.cmg.models.{ScheduleInfo, ScheduleInfoReply}

case class ScheduleStatusReply(
  code: Int = 0,
  message: String = "Success",
  status: Option[ScheduleStatus] = None
)

object ScheduleStatusReply {
  implicit val encodeScheduleStatusReply: JsonEncoder[ScheduleStatusReply] = DeriveJsonEncoder.gen[ScheduleStatusReply]
  implicit val decodeScheduleStatusReply: JsonDecoder[ScheduleStatusReply] = DeriveJsonDecoder.gen[ScheduleStatusReply]

  def apply(code: Int, message: String): ScheduleStatusReply = new ScheduleStatusReply(code, message)

  def apply(scheduleInfoReply: ScheduleInfoReply) = scheduleInfoReply.TotalCount match {
    case 0 => new ScheduleStatusReply(code = -1, message = s"No such schedules for vessel and voyage.")
    case 1 => {
      val scheduleStatus = transformToEntity(scheduleInfoReply.InnerList.head)
      new ScheduleStatusReply(status = Some(scheduleStatus))
    }
    case _ => new ScheduleStatusReply(code = -1, message = s"Find more than one schedules, please check your input. [${scheduleInfoReply.toJsonPretty}]")
  }

  def transformToEntity(s: ScheduleInfo): ScheduleStatus = ScheduleStatus(
      shipId = s.ShipId,
      fullName = s.TheFullName,
      inVoya = s.INBUSINESSVOY,
      outVoya = s.OUTBUSINESSVOY,
      pob = s.POB,
      etb = s.ETB,
      etd = s.ETD,
      inAgent = s.Inagent,
      outAgent = s.Outagent,
      notes = s.Notes)
}

trait VesselService {

  def fetchScheduleStatus(vessel: String, voyage: String): Task[ScheduleStatusReply]

  def fetchScheduleStatusDirectly(vessel: String, voyage: String): Task[ScheduleStatusReply]

  final def getScheduleStatus(vessel: String, voyage: String, wharf: String): Task[ScheduleStatusReply] = fetchScheduleStatus(vessel, voyage)

  final def getScheduleStatusDirectly(vessel: String, voyage: String, wharf: String): Task[ScheduleStatusReply] = fetchScheduleStatusDirectly(vessel, voyage)
}