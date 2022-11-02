package cc.knowship.subscribe.service

import zio._
import zio.http.Client

import cc.knowship.subscribe.AppConfig
import cc.knowship.subscribe.db.model.{Vessel, Voyage}
import cc.knowship.subscribe.wharf._
import cc.knowship.subscribe.util.Constants

trait WharfInformationServ {

  /**
    * 不同码头的判断情况不一样，只提供默认实现
    * @param vAged
    * @param vNew
    */
  def isChanged(vAged: Voyage, vNew: Voyage): Boolean = (vNew.atd != vAged.atd || vNew.etd != vAged.etd || vNew.etb != vAged.etb)

  /**
    * 不同码头的判断情况不一样，只提供默认实现
    * @param vNew
    */
  def isFinished(vNew: Voyage): Boolean = vNew.atd != Constants.DEFAULT_STRING_VALUE

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