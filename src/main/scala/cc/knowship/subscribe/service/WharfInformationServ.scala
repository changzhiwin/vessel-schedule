package cc.knowship.subscribe.service

import zio._
import zio.http.Client
import zio.http.html._

import cc.knowship.subscribe.AppConfig
import cc.knowship.subscribe.db.model.{Subscription, Vessel, Voyage, Wharf}
import cc.knowship.subscribe.wharf._
import cc.knowship.subscribe.util.{Constants, EmailTemplate}

trait WharfInformationServ {

  /**
    * 不同码头的判断情况不一样，只提供默认实现
    * @param vAged
    * @param vNew
    */
  //def isChanged(vAged: Voyage, vNew: Voyage): Boolean = (vNew.atd != vAged.atd || vNew.etd != vAged.etd || vNew.etb != vAged.etb)
  def hasChanged(vAged: Voyage, vNew: Voyage): Option[String] = {
    if (vNew.atd != vAged.atd) {
      Some(s"ATD [${vNew.atd}]")
    } else if (vNew.etd != vAged.etd) {
      Some(s"ETD [${vNew.etd}]")
    } else if (vNew.etb != vAged.etb) {
      Some(s"ETB [${vNew.etb}]")
    } else {
      None
    }
  }

  /**
    * 不同码头的判断情况不一样，只提供默认实现
    * @param vNew
    */
  //def isFinished(vNew: Voyage): Boolean = vNew.atd != Constants.DEFAULT_STRING_VALUE
  def hasFinished(vNew: Voyage): Option[String] = {
    (vNew.atd != Constants.DEFAULT_STRING_VALUE) match {
      case true  => Some(s"ATD [${vNew.atd}]")
      case false => None
    }
  }

  final def viewOfHtml(subscription: Subscription, voyage: Voyage, vessel: Vessel, wharf: Wharf): Html = {

    val extendInfos = Seq(
      "客户" -> subscription.infos,
      "码头"    -> wharf.name
    )

    val vesselInfos = Seq(
      "船名"    -> vessel.shipName,
      "中文名"  -> vessel.shipCnName,
      "UN代码"  -> vessel.unCode,
      "出口代理" -> vessel.outAgent,
      "船公司"   -> vessel.company
    )

    //val systemInfos = Seq("流水号"  -> subscription.id.toString())

    EmailTemplate.container(
      Seq(
        EmailTemplate.paragraph_2cols(extendInfos), 
        EmailTemplate.paragraph_hr,
        viewOfSchedule(voyage),
        EmailTemplate.paragraph_hr,
        EmailTemplate.paragraph_2cols(vesselInfos),
      )
    )
  }

  /**
    * 调度信息的部分显示视图不一样
    * @param voyage
    */
  def viewOfSchedule(voyage: Voyage): Dom = {
    val scheduleInfos = Seq(
      "出口航次" -> voyage.outVoy,
      "港区"    -> voyage.terminalCode,

      "计划靠泊" -> voyage.eta,
      "实际靠泊" -> voyage.ata,
      "计划离泊" -> voyage.etd,
      "实际离泊" -> voyage.atd,
      "附言"    -> voyage.notes
    )

    EmailTemplate.paragraph_2cols(scheduleInfos)
  }

  /**
    * 船名、航次不一定是正确的，用于匹配对应的航次
    * @param vesselName
    * @param voyageChoose 为空或者不配输入的场次，则尝试使用最新的航次
    * @return 返回正确的航次代码
    */
  def voyageOfVessel(vesselName: String, voyageName: String): Task[String]

  /**
    * 正确船名、航次，用于更新
    * @param vesselName
    * @param voyageCode
    */
  def voyageStatus(vesselName: String, voyageCode: String): Task[(Vessel, Voyage)]

}

// https://zio.dev/reference/contextual/zenvironment
object WharfInformationServ {

  val layer: ZLayer[Client with AppConfig, Nothing, Map[String, WharfInformationServ]] = {

    ZLayer.fromZIOEnvironment {
      for {
        client <- ZIO.service[Client]
        config <- ZIO.service[AppConfig]
      } yield {
        ZEnvironment(
          Map(
            "FAKE"  -> FakeWharfInformation(), 
            "CMG"   -> CmgWharfInformation(client, config),
            "NPEDI" -> NpediWharfInformation(client, config)
          )
        )
      }
    }
  }
    
}