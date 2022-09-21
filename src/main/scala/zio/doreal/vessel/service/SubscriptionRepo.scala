package zio.doreal.vessel.service

import zio._
import zio.doreal.vessel.models.{Subscription}

trait SubscriptionRepo {

  def register(shipmentId: String, userId: String): Task[Boolean]

  def unsubscribe(shipmentId: String, userId: String): Task[Boolean]

  def getAllSubscribers(shipmentId: String): Task[List[String]]
}

object SubscriptionRepo {
  def register(shipmentId: String, userId: String): ZIO[SubscriptionRepo, Throwable, Boolean] =
    ZIO.serviceWithZIO[SubscriptionRepo](_.register(shipmentId, userId))

  def unsubscribe(shipmentId: String, userId: String): ZIO[SubscriptionRepo, Throwable, Boolean] =
    ZIO.serviceWithZIO[SubscriptionRepo](_.unsubscribe(shipmentId, userId))

  def getAllSubscribers(shipmentId: String): ZIO[SubscriptionRepo, Throwable, List[String]] =
    ZIO.serviceWithZIO[SubscriptionRepo](_.getAllSubscribers(shipmentId))
}