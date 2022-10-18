package cc.knowship.subscribe.db.model

import java.util.UUID

import zio.json._

final case class Vessel(
  id: UUID,
  shipCode: String,   // ShipId 船代码
  shipName: String,   // TheFullName
  company: String,    // LINEID 船公司
  imo: String,        // IMO

  wharfId: UUID,      // 哪个码头的船，输入项
  createAt: Long,
)

object Vessel {
  implicit val codec: JsonCodec[Vessel] = DeriveJsonCodec.gen
}