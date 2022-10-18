package cc.knowship.subscribe.db.model

import java.util.UUID

import zio.json._

final case class Wharf(
  id: UUID,
  name: String,
  code: String,   // 简写代码
  
  createAt: Long,
)

object Wharf {
  implicit val codec: JsonCodec[Wharf] = DeriveJsonCodec.gen
}