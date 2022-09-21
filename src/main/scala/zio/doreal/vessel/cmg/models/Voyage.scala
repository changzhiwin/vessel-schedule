package zio.doreal.vessel.cmg.models

import zio.json._

// "TerminalCode": "CCT", "OutBoundVoy": "076W", "InBoundVoy": "076WI"
case class Voyage(TerminalCode: String, OutBoundVoy: String, InBoundVoy: String)

object Voyage {
  implicit val encodeVoyage: JsonEncoder[Voyage] = DeriveJsonEncoder.gen[Voyage]

  implicit val decodeVoyage: JsonDecoder[Voyage] = DeriveJsonDecoder.gen[Voyage]
}

case class ResponseVoyage(InnerList: List[Voyage], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

object ResponseVoyage {
  implicit val encodeResponseSchedule: JsonEncoder[ResponseVoyage] = DeriveJsonEncoder.gen[ResponseVoyage]

  implicit val decodeResponseSchedule: JsonDecoder[ResponseVoyage] = DeriveJsonDecoder.gen[ResponseVoyage]
}