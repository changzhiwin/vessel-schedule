package zio.doreal.vessel.services

import zio._
import zio.json._
import zio.http.{Client, Request, URL, Body}
import zio.http.model.{Headers, Method}
import zio.doreal.vessel.entities.User
import zio.doreal.vessel.dao.{UserDao, SubscriptionDao, ScheduleStatusDao}

trait PublishSubscriptService {

  def notify(shipmentId: String, scheduleStatusId: String): ZIO[Any, Throwable, Unit]

  def start(): ZIO[Any, Throwable, Unit]

  def stop(): ZIO[Any, Throwable, Unit]
}

object PublishSubscriptService {

  def start(): ZIO[PublishSubscriptService, Throwable, Unit] = 
    ZIO.serviceWithZIO[PublishSubscriptService](_.start())

  case class PublishSubscriptServiceLive(
      client: Client,
      queue: Queue[(String, String)], 
      userDao: UserDao, 
      subscriptionDao: SubscriptionDao, 
      scheduleStatusDao: ScheduleStatusDao) extends PublishSubscriptService {

    def notify(shipmentId: String, scheduleStatusId: String): ZIO[Any, Throwable, Unit] = queue.offer(shipmentId -> scheduleStatusId) *> ZIO.unit

    def start(): ZIO[Any, Throwable, Unit] = ZIO.whileLoop(true) {
      for {
        item <- queue.take.debug("Publish Queue Take: ")
        subscribes <- subscriptionDao.findByShipment(item._1)
        status <- scheduleStatusDao.findById(item._2)
        /*
        case (shipmentId, scheduleStatusId) <- queue.take.debug("Publish Queue Take: ")
        subscribes <- subscriptionDao.findByShipment(shipmentId)
        status <- scheduleStatusDao.findById(scheduleStatusId)
        */
        _ <- ZIO.foreachDiscard(subscribes) { sub =>
          for {
            user <- userDao.findById(sub.userId)
            _    <- pushToUser(user, s"Update notify: [${status.fullName}/${status.outVoya}]", status.toJsonPretty).debug("Notify result:")
          } yield ()
        }
      } yield ()
    } { _ => }

    def stop(): ZIO[Any, Throwable, Unit] = queue.shutdown

    private def pushToUser(user: User, title: String, body: String): ZIO[Any, Throwable, String] = {
      val headers = Headers("X-Email-To", user.openId) ++ Headers("X-Email-From", user.parent) ++ Headers("X-Email-Subject", title)
      for {
        url      <- ZIO.fromEither(URL.fromString("http://0.0.0.0:8080/notify")).orDie
        response <- client.request(
            Request(
              method = Method.POST, 
              url = url, 
              headers = headers, 
              body = Body.fromString(body)))   
        body <- response.body.asString  
      } yield body
    }
  }

  val live: ZLayer[Client with UserDao with SubscriptionDao with ScheduleStatusDao, Nothing, PublishSubscriptService] = ZLayer {
    for {
      client <- ZIO.service[Client]
      queue <- Queue.bounded[(String, String)](100)
      userDao <-  ZIO.service[UserDao]
      subscription <- ZIO.service[SubscriptionDao]
      status <- ZIO.service[ScheduleStatusDao]
    } yield PublishSubscriptServiceLive(client, queue, userDao, subscription, status)
  }
}