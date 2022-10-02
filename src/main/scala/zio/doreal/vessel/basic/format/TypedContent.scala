package zio.doreal.vessel.basic.format

trait TypedContent {

  def message: String

  def toText(): String = message

  def toHtml(): String = toText()
  
}

case class  DefaultContent(val message: String) extends TypedContent {}