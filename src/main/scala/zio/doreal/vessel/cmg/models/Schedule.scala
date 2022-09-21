package zio.doreal.vessel.cmg.models

import zio.json._
import zio.doreal.vessel.entities.ScheduleStatus

case class CmgSchedule(
  TerminalCode: String, //"CCT",
  ETADate: String, //"2022-09-20 11:00",
  ServiceId: String, //"IFX",
  InServiceId: String, //"IFX",
  OutServiceId: String, //"IFX",
  ShipId: String, //"XTJN",
  TheFullName: String, //"XIN TIAN JIN",
  LINEID: String, //"COS",
  invoynbr: String, //"076WI",
  outvoynbr: String, //"076W",
  POB: String, //"2022-09-21 00:30",
  ETB: String, //"2022-09-21",
  ETD: String, //"2022-09-21 19:30",
  ATA: Option[String], //null,
  ATD: Option[String], //null,
  Notes: String, //"已开始收箱",
  IMO: String, //"UN9234343",
  INBUSINESSVOY: String, //"076WI",
  OUTBUSINESSVOY: String, //"076W",
  Inagent: String, //"COS",
  Outagent: String //"COS"
)

object CmgSchedule {
  implicit val encodeSchedule: JsonEncoder[CmgSchedule] = DeriveJsonEncoder.gen[CmgSchedule]

  implicit val decodeSchedule: JsonDecoder[CmgSchedule] = DeriveJsonDecoder.gen[CmgSchedule]

  def toScheduleStatus(s: CmgSchedule): ScheduleStatus = {
    ScheduleStatus(
      shipId = s.ShipId,
      fullName = s.TheFullName,
      inVoya = s.INBUSINESSVOY,
      outVoya = s.OUTBUSINESSVOY,
      pob = s.POB,
      etb = s.ETB,
      etd = s.ETD,
      inAgent = s.Inagent,
      outAgent = s.Outagent,
      notes = s.Notes
    )
  }
}

case class ResponseSchedule(InnerList: List[CmgSchedule], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

object ResponseSchedule {
  implicit val encodeResponseVoyage: JsonEncoder[ResponseSchedule] = DeriveJsonEncoder.gen[ResponseSchedule]

  implicit val decodeResponseVoyage: JsonDecoder[ResponseSchedule] = DeriveJsonDecoder.gen[ResponseSchedule]
}