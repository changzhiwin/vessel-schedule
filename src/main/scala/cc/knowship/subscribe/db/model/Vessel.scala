package cc.knowship.subscribe.db.model

import java.util.UUID

import zio.json._

final case class Vessel(
  id: UUID,
  shipCode: String,   // ShipId 船代码
  shipName: String,   // TheFullName
  shipCnName: String,  // 中文名
  company: String,    // LINEID 船公司
  unCode: String,        // UN代码 IMO
  inAgent: String,       // Inagent
  outAgent: String,      // Outagent

  wharfId: UUID,      // 哪个码头的船，输入项
  createAt: Long,
)

object Vessel {
  implicit val codec: JsonCodec[Vessel] = DeriveJsonCodec.gen
}