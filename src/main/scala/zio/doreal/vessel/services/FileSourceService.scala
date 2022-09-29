package zio.doreal.vessel.services

import zio._
import zio.doreal.vessel.entities._
import zio.doreal.vessel.dao._
import zio.doreal.vessel.utils.FileHelper

trait FileSourceService {

  def needSync: ZIO[Any, Nothing, Unit]

  def init(): ZIO[Any, Throwable, Unit]

  def sync(): ZIO[Any, Throwable, Unit]
}

object FileSourceService {

  def init(): ZIO[FileSourceService, Throwable, Unit] = 
    ZIO.serviceWithZIO[FileSourceService](_.init())

  def sync(): ZIO[FileSourceService, Throwable, Unit] =
    ZIO.serviceWithZIO[FileSourceService](_.sync())

  val live: ZLayer[UserDao with SubscriptionDao with ScheduleStatusDao with ShipmentDao, Nothing, FileSourceService] = ZLayer {
    for {
      changed <- Ref.make(false)
      userDao <-  ZIO.service[UserDao]
      subscription <- ZIO.service[SubscriptionDao]
      status <- ZIO.service[ScheduleStatusDao]
      shipment <- ZIO.service[ShipmentDao]
    } yield FileSourceServiceLive(changed, userDao, subscription, status, shipment)
  }

  case class FileSourceServiceLive(
      changed: Ref[Boolean],
      userDao: UserDao,
      subscriptionDao: SubscriptionDao,
      scheduleStatusDao: ScheduleStatusDao,
      shipmentDao: ShipmentDao
      ) extends FileSourceService {
    
    def needSync: ZIO[Any, Nothing, Unit] = changed.update(_ => true).debug("Need Sync: ")

    def init(): ZIO[Any, Throwable, Unit] = for {
      users <- FileHelper.readList[User]()
      _     <- userDao.init(users)
      subscriptions <- FileHelper.readList[Subscription]()
      _     <- subscriptionDao.init(subscriptions)
      scheduleStatues <- FileHelper.readList[ScheduleStatus]()
      _     <- scheduleStatusDao.init(scheduleStatues)
      shipments <- FileHelper.readList[Shipment]()
      _     <- shipmentDao.init(shipments)
    } yield ()

    def sync(): ZIO[Any, Throwable, Unit] = for {
      isChanged <- changed.get.debug("Changed: ")
      _         <- ZIO.when(isChanged) {
        for {
          users <- userDao.getAll()
          _     <- FileHelper.writeList[User](users)
          subscriptions <- subscriptionDao.getAll()
          _     <- FileHelper.writeList[Subscription](subscriptions)
          scheduleStatues <- scheduleStatusDao.getAll()
          _     <- FileHelper.writeList[ScheduleStatus](scheduleStatues)
          shipments <- shipmentDao.getAll()
          _     <- FileHelper.writeList[Shipment](shipments)
          _ <- changed.update(_ => false)
        } yield ()
      }
    } yield ()
  }
}