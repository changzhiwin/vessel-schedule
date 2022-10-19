package cc.knowship.subscribe.service

import java.util.UUID

import zio._

import cc.knowship.subscribe.db.model.Subscription

trait SubscribeServ {

  def tryIt(subscriberId: UUID, wharf: String, vessel: String, voyage: String, infos: String): Task[Option[Subscription]]

  def cancel(subscriptionId: UUID): Task[Unit]
}