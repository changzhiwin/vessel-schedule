package cc.knowship.subscribe.db.model

import java.util.UUID

import zio.json._

/**
  * @param id
  * @param openId   emailId/wxId
  * @param source   email/wx/qw
  * @param receiver 接收方，如公众号、公开邮箱
  */
final case class Subscriber(
  id: UUID, 
  openId: String, 
  source: String, 
  receiver: String,
  nickname: String, 

  createAt: Long, 
  updateAt: Long,
)

object Subscriber {
  implicit val codec: JsonCodec[Subscriber] = DeriveJsonCodec.gen
}
