package zio.doreal.vessel.services

import zio._
import zio.json._
import zio.http.{Client, Request, URL, Body}
import zio.http.model.{Headers, Method}
import zio.doreal.vessel.AppConfig
import zio.doreal.vessel.entities.User
import zio.doreal.vessel.dao.{UserDao, SubscriptionDao, ScheduleStatusDao, ShipmentDao}
import zio.doreal.vessel.basic.format.TypedContent
import zio.doreal.vessel.services.format._

trait PublishSubscriptService {

  def notify(shipmentId: String, scheduleStatusId: String, detail: String): ZIO[Any, Throwable, Unit]

  def start(): ZIO[Any, Throwable, Unit]

  def stop(): ZIO[Any, Throwable, Unit]
}

object PublishSubscriptService {

  def start(): ZIO[PublishSubscriptService, Throwable, Unit] = 
    ZIO.serviceWithZIO[PublishSubscriptService](_.start())

  case class PublishSubscriptServiceLive(
      config: AppConfig,
      client: Client,
      queue: Queue[(String, String, String)], 
      userDao: UserDao, 
      subscriptionDao: SubscriptionDao, 
      shipmentDao: ShipmentDao,
      scheduleStatusDao: ScheduleStatusDao) extends PublishSubscriptService {

    def notify(shipmentId: String, scheduleStatusId: String, detail: String): ZIO[Any, Throwable, Unit] = queue.offer( (shipmentId, scheduleStatusId, detail) ) *> ZIO.unit

    def start(): ZIO[Any, Throwable, Unit] = ZIO.whileLoop(true) {
      for {
        item <- queue.take.debug("Publish Queue Take: ")
        subscribes <- subscriptionDao.findByShipment(item._1)
        status <- scheduleStatusDao.findById(item._2)
        _ <- ZIO.foreachDiscard(subscribes) { sub =>
          for {
            user <- userDao.findById(sub.userId)
            title <- status.atd match {
              case "-" => ZIO.succeed(s"Update notify: ${item._3}")
              case _: String => ZIO.succeed(s"Finish notify: ${item._3}")
            }
            _ <- pushToUser(user, title, SubscribeContent(status = Some(status), extraInfo = Some(sub.extraInfo))).debug("Notify result:")
          } yield ()
        }
        // check atd: if some, delete (all subscription /a shipment /a scheduleStatus)
        _ <- (
          ZIO.log(s"End subscription: shipment [${item._1}], status ${status.toJson}") *>
          subscriptionDao.deleteByShipment(item._1) *> 
          shipmentDao.delete(item._1) *> 
          scheduleStatusDao.delete(item._2) 
        ).when(status.atd != "-")
      } yield ()
    } { _ => }

    def stop(): ZIO[Any, Throwable, Unit] = queue.shutdown

    private def pushToUser(user: User, title: String, content: TypedContent): ZIO[Any, Throwable, String] = {
      val headers = Headers("X-Email-To", user.openId) ++ 
        Headers("X-Email-Bcc", "bebest@88.com") ++
        Headers("X-Email-From", user.parent) ++ 
        Headers("X-Email-Subject", title)
      for {
        url      <- ZIO.fromEither(URL.fromString(config.notifyUrl)).orDie
        response <- client.request(
            Request(
              method = Method.POST, 
              url = url, 
              headers = headers, 
              body = Body.fromString(content.toHtml())))
        body <- response.body.asString  
      } yield body
    }
  }

  val live: ZLayer[AppConfig with Client with UserDao with SubscriptionDao with ShipmentDao with ScheduleStatusDao, Nothing, PublishSubscriptService] = ZLayer {
    for {
      config <- ZIO.service[AppConfig]
      client <- ZIO.service[Client]
      queue <- Queue.bounded[(String, String, String)](100)
      userDao <-  ZIO.service[UserDao]
      subscription <- ZIO.service[SubscriptionDao]
      shipmentDao <- ZIO.service[ShipmentDao]
      status <- ZIO.service[ScheduleStatusDao]
    } yield PublishSubscriptServiceLive(config, client, queue, userDao, subscription, shipmentDao, status)
  }
}