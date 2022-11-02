package cc.knowship.subscribe.service

import zio._

import cc.knowship.subscribe.db.model.Subscriber
import cc.knowship.subscribe.db.table.SubscriberTb

trait SubscriberServ {

  def findOrCreate(openId: String, source: String, receiver: String, nickname: String): Task[Subscriber]
}

case class SubscriberServLive(subscriberTb: SubscriberTb) extends SubscriberServ {

  def findOrCreate(openId: String, source: String, receiver: String, nickname: String): Task[Subscriber] =
    subscriberTb.findByOpenId(openId, source)
      .someOrElseZIO {
        subscriberTb.create(openId, source, receiver, nickname)
      }
}

object SubscriberServLive {
  val layer = ZLayer.fromFunction(SubscriberServLive.apply _)
}