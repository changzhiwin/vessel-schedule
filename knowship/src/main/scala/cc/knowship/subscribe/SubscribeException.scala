package cc.knowship.subscribe

sealed trait SubscribeException extends Exception

object SubscribeException {

  case class DbRecordNotFound(detail: String) extends SubscribeException

  case class VesselNoVoyageFound(detail: String) extends SubscribeException

  case class VoyageMustOnlyOne(detail: String) extends SubscribeException

  case class JsonDecodeFailed(detail: String) extends SubscribeException

  case class URLParseFailed(detail: String) extends SubscribeException
}