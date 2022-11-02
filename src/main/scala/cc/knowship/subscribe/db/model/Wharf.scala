package cc.knowship.subscribe.db.model

import java.util.UUID

import zio.json._

final case class Wharf(
  id: UUID,
  name: String,
  code: String,    // 简写代码
  website: String,
  
  period: Long,    // 轮询的时间间隔，毫秒为单位
  workStart: Long, // 开始工作的时间，以零点为基准的相对毫秒数
  workEnd: Long,   // 停止工作的时间
  createAt: Long,
)

object Wharf {
  implicit val codec: JsonCodec[Wharf] = DeriveJsonCodec.gen
}