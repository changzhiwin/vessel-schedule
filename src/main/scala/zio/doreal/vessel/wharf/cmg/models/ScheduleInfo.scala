package zio.doreal.vessel.wharf.cmg.models

import zio.json._
//import zio.doreal.vessel.entities.ScheduleStatus

case class ScheduleInfo(
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

object ScheduleInfo {
  implicit val encodeScheduleInfo: JsonEncoder[ScheduleInfo] = DeriveJsonEncoder.gen[ScheduleInfo]

  implicit val decodeScheduleInfo: JsonDecoder[ScheduleInfo] = DeriveJsonDecoder.gen[ScheduleInfo]
}

case class ScheduleInfoReply(InnerList: List[ScheduleInfo], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

object ScheduleInfoReply {
  implicit val encodeScheduleInfoReply: JsonEncoder[ScheduleInfoReply] = DeriveJsonEncoder.gen[ScheduleInfoReply]

  implicit val decodeScheduleInfoReply: JsonDecoder[ScheduleInfoReply] = DeriveJsonDecoder.gen[ScheduleInfoReply]
}