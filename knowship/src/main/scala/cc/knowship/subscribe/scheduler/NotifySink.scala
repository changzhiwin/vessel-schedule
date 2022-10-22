package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

import cc.knowship.subscribe.db.model.{Subscription}
import cc.knowship.subscribe.service.SubscribeServ

case class NotifySink(subscribeServ: SubscribeServ) {
  
  def consume: ZSink[Any, Throwable, Subscription, Nothing, Unit] = ZSink.foreach(sub => subscribeServ.pushUpdate(sub))
}

object NotifySink {
  val layer = ZLayer.fromFunction(NotifySink.apply _)
}