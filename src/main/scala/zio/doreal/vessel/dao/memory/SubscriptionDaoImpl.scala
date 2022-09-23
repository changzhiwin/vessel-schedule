package zio.doreal.vessel.dao.memory

import zio._
import zio.doreal.vessel.entities.Subscription
import zio.doreal.vessel.dao.SubscriptionDao

case class SubscriptionDaoImpl(repo: Ref[List[Subscription]]) extends SubscriptionDao {

  //0 -> exist; 1 -> add
  def subscribe(userId: String, shipmentId: String): Task[Int] = for {
    table <- repo.get
    added <- table.find(row => (row.userId == userId && row.shipmentId == shipmentId)) match {
      case Some(_) => ZIO.succeed(0)
      case None => for {
        id <- Random.nextUUID.map(_.toString())
        _  <- repo.update(_.appended(Subscription(id, userId, shipmentId)))
      } yield (1)
    }
  } yield added

  def unsubscribe(userId: String, shipmentId: String): Task[Int] = for {
    //_ <- repo.update(_.filter(row => (row.userId != userId || row.shipmentId != shipmentId)))
    added <- repo.modify(t => {
      val oldSize = t.size
      val newTb = t.filter(row => (row.userId != userId || row.shipmentId != shipmentId))
      val newSize = newTb.size
      (oldSize - newSize, newTb)
    })
  } yield added

  def noSubscriberExist(shipmentId: String): Task[Boolean] = for {
    table <- repo.get
    ret   <- table.find(row => (row.shipmentId == shipmentId)) match {
      case Some(_) => ZIO.succeed(false)
      case None    => ZIO.succeed(true)
    }
  } yield ret
    
  def getAll(): Task[List[Subscription]] = repo.get
}

object SubscriptionDaoImpl {

  def live: ULayer[SubscriptionDao] =
    ZLayer {
      Ref.make(List.empty[Subscription]).map(SubscriptionDaoImpl.apply)
    }
}