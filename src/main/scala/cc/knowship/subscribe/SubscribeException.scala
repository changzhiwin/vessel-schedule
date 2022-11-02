package cc.knowship.subscribe

sealed abstract class SubscribeException(msg: String) extends Exception(msg)

object SubscribeException {

  final case class DbRecordNotFound(detail: String) extends SubscribeException(detail)

  final case class VesselNoVoyageFound(detail: String) extends SubscribeException(detail)

  final case class VoyageMustOnlyOne(detail: String) extends SubscribeException(detail)

  final case class JsonDecodeFailed(detail: String) extends SubscribeException(detail)

  final case class URLParseFailed(detail: String) extends SubscribeException(detail)

  final case class WharfInfServNotFound(detail: String) extends SubscribeException(detail)
}