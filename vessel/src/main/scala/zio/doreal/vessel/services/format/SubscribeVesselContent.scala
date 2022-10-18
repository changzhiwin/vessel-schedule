package zio.doreal.vessel.services.format

import zio.json._
import zio.http.html._

import zio.doreal.vessel.basic.format.TypedContent
import zio.doreal.vessel.entities.{ScheduleStatus, User, Shipment}
import zio.doreal.vessel.utils.EmailTemplate

case class SubscribeContent(val message: String = "Success", status: Option[ScheduleStatus] = None, extraInfo: Option[String] = None) extends TypedContent {

  override def toText(): String = {
    message + "\r\n" + extraInfo + "\r\n" + status.toJsonPretty
  }

  override def toHtml(): String = {
    val sysList = Seq(("系统消息", message))
    val sysTr = EmailTemplate.paragraph_2cols(sysList)

    val extraTr = extraInfo match {
      case None => Seq.empty[Dom]
      case Some(ext) => {
        val extList = Seq("客户信息" -> ext)
        Seq(EmailTemplate.paragraph_2cols(extList), EmailTemplate.paragraph_hr)
      }
    }

    val appTr = status match {
      case None     => Seq.empty[Dom]
      case Some(st) => {
        val appList = Seq(
          "港区" -> st.terminalCode,
          "ETA" -> st.eta,
          "进口航线	" -> st.inService,
          "出口航线" -> st.outService,
          "船代码" -> st.shipCode,
          "船名" -> st.shipName,
          "船公司" -> st.company,
          "进口航次" -> st.inVoy,
          "出口航次" -> st.outVoy,
          "POB" -> st.pob,
          "ETB" -> st.etb,
          "ETD" -> st.etd,
          "ATA" -> st.ata,
          "ATD" -> st.atd,
          "备注" -> st.notes,
          "IMO" -> st.imo,
          "进口商业航次码" -> st.inBusiVoy,
          "出口商业航次码" -> st.outBusiVoy,
          "进口代理" -> st.inAgent,
          "出口代理" -> st.outAgent,
        )
        Seq(EmailTemplate.paragraph_2cols(appList), EmailTemplate.paragraph_hr)
      }
    }

    EmailTemplate.emailHtml( extraTr ++ appTr ++ Seq(sysTr) )
  }
}

case class UnSubscribeContent(val message: String, rawSubscribe: String) extends TypedContent {
  
  override def toText(): String = s"""{"message":"${message}", "notice": "${rawSubscribe}"}"""

  override def toHtml(): String = {
    val sysList = Seq(("系统消息", message), ("提示", rawSubscribe))
    val sysTr = EmailTemplate.paragraph_2cols(sysList)

    EmailTemplate.emailHtml(Seq(sysTr))
  }
}

case class AdminContent(val message: String = "", userList: List[User], shipmentList: List[Shipment], subDetails: List[String]) extends TypedContent {

  override def toText(): String = s"Total ${userList.size} users.\r\n" + 
    userList.map(_.debugPrint).mkString("\r\n") + "\r\n" +
    s"Total ${shipmentList.size} shipments.\r\n" + 
    shipmentList.map(_.debugPrint).mkString("\r\n") + "\r\n" +
    s"Total ${subDetails.size} subscribe relations.\r\n" +
    subDetails.sorted.mkString("\r\n") 

  override def toHtml(): String = {

    val userSize = EmailTemplate.paragraph(s"Total ${userList.size} users.")
    val userTr = EmailTemplate.paragraph_ul(userList.map(_.debugPrint).toSeq)
    val shipmentSize = EmailTemplate.paragraph(s"Total ${shipmentList.size} shipments.")
    val shipmentTr = EmailTemplate.paragraph_ul(shipmentList.map(_.debugPrint).toSeq)
    val subscribeSize = EmailTemplate.paragraph(s"Total ${subDetails.size} subscribe relations.")
    val subscribeTr = EmailTemplate.paragraph_ul(subDetails.sorted.toSeq)

    EmailTemplate.emailHtml(Seq(userSize, userTr, shipmentSize, shipmentTr, subscribeSize, subscribeTr))
  }
}