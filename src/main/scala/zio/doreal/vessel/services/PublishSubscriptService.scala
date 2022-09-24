package zio.doreal.vessel.services

import zio._
import zio.doreal.vessel.dao.ShipmentDao

trait PublishSubscriptService {

  def notify(shipmentId: String, scheduleStatusId: String): ZIO[Any, Throwable, Unit]

  def start(): ZIO[Any, Throwable, Unit]

  def stop(): ZIO[Any, Throwable, Unit]
}

object PublishSubscriptService {

  def start(): ZIO[PublishSubscriptService, Throwable, Unit] = 
    ZIO.serviceWithZIO[PublishSubscriptService](_.start())

  case class PublishSubscriptServiceLive(queue: Queue[(String, String)], subscriptionDao: SubscriptionDao, scheduleStatusDao: ScheduleStatusDao) extends PublishSubscriptService {

    def notify(shipmentId: String, scheduleStatusId: String): ZIO[Any, Throwable, Unit] = queue.offer(shipmentId -> scheduleStatusId)

    def start(): ZIO[Any, Throwable, Unit] = ZIO.whileLoop(true) {
      for {
        (shipmentId, scheduleStatusId) <- queue.take.debug("Publish queue take: ")
        subscribes <- subscriptionDao.findByShipment(shipmentId)
        status <- scheduleStatusDao.findById(scheduleStatusId)
        _ <- ZIO.foreachDiscard(subscribes) { sub =>
          for {
            user <- userDao.findById(sub.userId)
            _ <- TODO.notify()
          } yield ()
        }
      } yield ()
    } { _ => }

    def stop(): ZIO[Any, Throwable, Unit] = queue.shutdown
  }

  val live: ZLayer[SubscriptionDao with ScheduleStatusDao, Nothing, PublishSubscriptService] = ZLayer {
    for {
      queue <- Queue.bounded[(String, String)](100)
      subscription <- ZIO.service[SubscriptionDao]
      status <- ZIO.service[ScheduleStatusDao]
    } yield PublishSubscriptService(queue, subscription, status)
  }
}