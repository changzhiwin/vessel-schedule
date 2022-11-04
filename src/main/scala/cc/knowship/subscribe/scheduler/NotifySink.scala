package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

import cc.knowship.subscribe.SubscribeChange
import cc.knowship.subscribe.service.SubscribeServ

case class NotifySink(subscribeServ: SubscribeServ) {
  
  def consume: ZSink[Any, Throwable, SubscribeChange, Nothing, Unit] = ZSink.foreach(change => subscribeServ.noitfy(change))
}

object NotifySink {
  val layer = ZLayer.fromFunction(NotifySink.apply _)
}