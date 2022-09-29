package zio.doreal.vessel.wharf.cmg.models

import zio.json._
//import zio.doreal.vessel.entities.ScheduleStatus

case class ScheduleInfo(
  TerminalCode: String, //"CCT",
  ETADate: Option[String], //"2022-09-20 11:00",
  ServiceId: Option[String], //"IFX",
  InServiceId: Option[String], //"IFX",
  OutServiceId: Option[String], //"IFX",
  ShipId: String, //"XTJN",
  TheFullName: String, //"XIN TIAN JIN",
  LINEID: String, //"COS",
  invoynbr: String, //"076WI",
  outvoynbr: String, //"076W",
  POB: Option[String], //"2022-09-21 00:30",
  ETB: Option[String], //"2022-09-21",
  ETD: Option[String], //"2022-09-21 19:30",
  ATA: Option[String], //null,
  ATD: Option[String], //null, // 有这个属性后，可以不在关注动态
  Notes: Option[String], //"已开始收箱",
  IMO: Option[String], //"UN9234343",
  INBUSINESSVOY: Option[String], //"076WI",
  OUTBUSINESSVOY: Option[String], //"076W",
  Inagent: Option[String], //"COS",
  Outagent: Option[String] //"COS"
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