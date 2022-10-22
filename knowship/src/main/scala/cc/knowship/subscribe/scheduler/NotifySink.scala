package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

import cc.knowship.subscribe.db.model.{Subscription}
//import cc.knowship.subscribe.db.table.{VoyageTb, SubscriptionTb, VesselTb, WharfTb}

case class NotifySink() {

  def consume: ZSink[Any, Exception, Subscription, Nothing, Unit] = ZSink.foreach { subscription =>
    Console.printLine(s">>>Notify: ${subscription.id}, ${subscription.infos}")
  }
}

object NotifySink {
  val layer = ZLayer.fromFunction(NotifySink.apply _)
}