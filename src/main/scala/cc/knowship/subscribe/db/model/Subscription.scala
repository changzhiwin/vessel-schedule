package cc.knowship.subscribe.db.model

import java.util.UUID

import zio.json._
final case class Subscription(
  id: UUID,
  subscriberId: UUID,
  voyageId: UUID,
  infos: String,
  createAt: Long,
  notifyAt: Long,
)

object Subscription {
  implicit val codec: JsonCodec[Subscription] = DeriveJsonCodec.gen
}