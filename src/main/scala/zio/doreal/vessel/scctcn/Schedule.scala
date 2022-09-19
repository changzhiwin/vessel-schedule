package zio.doreal.vessel.scctcn

import zio.json._

case class Schedule(
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

object Schedule {
  implicit val encodeSchedule: JsonEncoder[Schedule] = DeriveJsonEncoder.gen[Schedule]

  implicit val decodeSchedule: JsonDecoder[Schedule] = DeriveJsonDecoder.gen[Schedule]
}

case class Voyage(TerminalCode: String, OutBoundVoy: String, InBoundVoy: String)

object Voyage {
  implicit val encodeVoyage: JsonEncoder[Voyage] = DeriveJsonEncoder.gen[Voyage]

  implicit val decodeVoyage: JsonDecoder[Voyage] = DeriveJsonDecoder.gen[Voyage]
}

case class ResponseSchedule(InnerList: List[Voyage], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

object ResponseSchedule {
  implicit val encodeResponseSchedule: JsonEncoder[ResponseSchedule] = DeriveJsonEncoder.gen[ResponseSchedule]

  implicit val decodeResponseSchedule: JsonDecoder[ResponseSchedule] = DeriveJsonDecoder.gen[ResponseSchedule]
}

case class ResponseVoyage(InnerList: List[Schedule], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

object ResponseVoyage {
  implicit val encodeResponseVoyage: JsonEncoder[ResponseVoyage] = DeriveJsonEncoder.gen[ResponseVoyage]

  implicit val decodeResponseVoyage: JsonDecoder[ResponseVoyage] = DeriveJsonDecoder.gen[ResponseVoyage]
}