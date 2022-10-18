package zio.doreal.vessel.wharf.cmg.models

import zio.json._

// Example: "TerminalCode": "CCT", "OutBoundVoy": "076W", "InBoundVoy": "076WI"
case class Voyage(TerminalCode: String, OutBoundVoy: String, InBoundVoy: String)

object Voyage {
  implicit val encodeVoyage: JsonEncoder[Voyage] = DeriveJsonEncoder.gen[Voyage]

  implicit val decodeVoyage: JsonDecoder[Voyage] = DeriveJsonDecoder.gen[Voyage]
}

case class VoyageReply(InnerList: List[Voyage], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

object VoyageReply {
  implicit val encodeVoyageReply: JsonEncoder[VoyageReply] = DeriveJsonEncoder.gen[VoyageReply]

  implicit val decodeVoyageReply: JsonDecoder[VoyageReply] = DeriveJsonDecoder.gen[VoyageReply]
}