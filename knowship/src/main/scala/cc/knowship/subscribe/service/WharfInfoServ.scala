package cc.knowship.subscribe.service

import zio._

import cc.knowship.subscribe.db.model.{Vessel, Voyage}

trait WharfInfoServ {

  /**
    * 船名、航次不一定是正确的，用于匹配对应的航次
    * @param vesselName
    * @param voyageChoose 为空或者不配输入的场次，则尝试使用最新的航次
    * @return 返回正确的航次代码
    */
  def voyageOfVessel(vesselName: String, voyageChoose: Option[String]): Task[Option[String]]

  /**
    * 正确船名、航次，用于更新
    * @param vesselName
    * @param voyageCode
    */
  def voyageStatus(vesselName: String, voyageCode: String): Task[Voyage]
}