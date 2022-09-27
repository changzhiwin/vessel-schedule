package zio.doreal.vessel.controls.vo

import zio.http.{QueryParams}

import zio.doreal.vessel.entities.User

case class SubscribeParams(
    channel: String,
    action: String, 
    vessel: String, 
    voyage: String, 
    wharf: String,
    from: String,
    to: String) {

  def getQueryKey: String = s"[${vessel}/${voyage}/${wharf}]"

  def getSubscribeInfo: String = s"[${vessel}/${voyage}]"

  def getUserWithOutId: User = User(id = "", openId = from, channel = channel, parent = to)

  def debugPrint: String = s"[${action} | ${vessel} | ${voyage} | ${from} | ${to}]"
}

object SubscribeParams {

  def fromEmailChannel(querys: QueryParams): SubscribeParams = {

    val action: String = querys.get("action").map(_.head).getOrElse("SUBSCRIBE").toUpperCase
    val vessel = querys.get("vessel").map(_.head).getOrElse("-")
    val voyage = querys.get("voyage").map(_.head).getOrElse("-")
    val wharf = querys.get("wharf").map(_.head).getOrElse("CMG")
    val from = querys.get("from").map(_.head).get
    val to   = querys.get("to").map(_.head).get

    SubscribeParams("EMAIL", action, vessel, voyage, wharf, from, to)
  }
}