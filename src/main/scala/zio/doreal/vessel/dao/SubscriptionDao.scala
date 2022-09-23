package zio.doreal.vessel.dao

import zio._
import zio.doreal.vessel.entities.{Subscription}

trait SubscriptionDao {

  // -1 -> error; 0 -> exist; 1 -> add
  def subscribe(userId: String, shipmentId: String): Task[Int]

  def unsubscribe(userId: String, shipmentId: String): Task[Int]

  def noSubscriberExist(shipmentId: String): Task[Boolean]

  def getAll(): Task[List[Subscription]]

}