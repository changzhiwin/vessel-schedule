package cc.knowship.subscribe

sealed trait SubscribeException extends Exception

object SubscribeException {

  final case class DbRecordNotFound(detail: String) extends SubscribeException

  final case class VesselNoVoyageFound(detail: String) extends SubscribeException

  final case class VoyageMustOnlyOne(detail: String) extends SubscribeException

  final case class JsonDecodeFailed(detail: String) extends SubscribeException

  final case class URLParseFailed(detail: String) extends SubscribeException

  final case class WharfInfServNotFound(detail: String) extends SubscribeException
}