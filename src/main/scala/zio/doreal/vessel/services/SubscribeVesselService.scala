package zio.doreal.vessel.services

import zio._
import zio.http._
import zio.json._

import zio.doreal.vessel.controls.vo.SubscribeParams
import zio.doreal.vessel.dao._
import zio.doreal.vessel.services.FileSourceService
import zio.doreal.vessel.wharf.VesselService
import zio.doreal.vessel.entities.ScheduleStatus

trait SubscribeVesselService {

  def subscribe(params: SubscribeParams): ZIO[Any, Throwable, String]

  def unsubscribe(params: SubscribeParams): ZIO[Any, Throwable, String] 

  def admin(params: SubscribeParams): ZIO[Any, Throwable, String]
}

case class SubscribeVesselServiceLive(
    userDao: UserDao, 
    shipmentDao: ShipmentDao, 
    subscriptionDao: SubscriptionDao,
    scheduleStatusDao: ScheduleStatusDao,
    vesselService: VesselService,
    fileSourceService: FileSourceService) extends SubscribeVesselService {

 def subscribe(params: SubscribeParams): ZIO[Any, Throwable, String] = for {
    user        <- userDao.findOrCreate(params.getUserWithOutId)
    shipmentOpt <- shipmentDao.findByQueryKey(params.getQueryKey)
    result      <- shipmentOpt match {
      case Some(shipment) => {
        for {
          added  <- subscriptionDao.subscribe(user.id, shipment.id).debug("Subscribe: ")
          status <- scheduleStatusDao.findById(shipment.scheduleStatusId)
        } yield status.toJsonPretty
      }
      case None => {
        for {
          scheduleStatusReply <- vesselService.getScheduleStatus(params.vessel, params.voyage, params.wharf).debug("Remote fetch: ")
          freshResult         <- scheduleStatusReply.code match {
            case 0 => for {
              _ <- ZIO.unit

              scheduleStatus = scheduleStatusReply.status.get
              scheduleStatusId <- scheduleStatusDao.save(scheduleStatus)

              realVoyage = scheduleStatus.outVoy
              shipmentId       <- shipmentDao.save(params.getQueryKey, params.vessel, realVoyage, params.wharf, scheduleStatusId)
              _                <- subscriptionDao.subscribe(user.id, shipmentId)
              status           <- scheduleStatusDao.findById(scheduleStatusId)
            } yield status.toJsonPretty
            case _ => ZIO.succeed(s"""{"message":"${scheduleStatusReply.message}"}""")
          }
        } yield freshResult
      }
    }

    _    <- fileSourceService.needSync

    // TODO: some rendering
    body <- ZIO.succeed(result)
  } yield body

  def unsubscribe(params: SubscribeParams): ZIO[Any, Throwable, String] = for {
    shipmentOpt <- shipmentDao.findByQueryKey(params.getQueryKey)
    result      <- shipmentOpt match {
      case Some(shipment) => for {
        user  <- userDao.findOrCreate(params.getUserWithOutId)
        added <- subscriptionDao.unsubscribe(user.id, shipment.id)
        // TODO: if added == 0, no need delete operation
        yes   <- subscriptionDao.noSubscriberExist(shipment.id)
        _     <- if (yes) scheduleStatusDao.delete(shipment.scheduleStatusId) *> shipmentDao.delete(shipment.id) else ZIO.unit
        tips  <- if (added == 0) ZIO.succeed("You don't have subscribe") else fileSourceService.needSync *> ZIO.succeed("Unsubscribe success")
      } yield s"""{"message":"${tips}, ${params.getSubscribeInfo}."}"""
      case None => ZIO.succeed(s"""{"message":"Not exist, ${params.getSubscribeInfo}."}""")
    }
  } yield result

  def admin(params: SubscribeParams): ZIO[Any, Throwable, String] = for {
    userList <- userDao.getAll()
    shipmentList <- shipmentDao.getAll()
    subscribeList <- subscriptionDao.getAll()
    detailList <- ZIO.foreach(subscribeList) { sub =>
      for {
        user     <- userDao.findById(sub.userId)
        shipment <- shipmentDao.findById(sub.shipmentId)
      } yield s"[${user.openId}] -> ${shipment.queryKey}"
    }
  } yield s"Total ${userList.size} users.\r\n" + 
    userList.map(_.debugPrint).mkString("\r\n") + "\r\n" +
    s"Total ${shipmentList.size} shipments.\r\n" + 
    shipmentList.map(_.debugPrint).mkString("\r\n") + "\r\n" +
    s"Total ${detailList.size} subscribe relations.\r\n" +
    detailList.sorted.mkString("\r\n") 
}

object SubscribeVesselServiceLive {
  val live: ZLayer[UserDao with ShipmentDao with SubscriptionDao with ScheduleStatusDao with VesselService with FileSourceService, Nothing, SubscribeVesselService] = ZLayer {
    for {
      user <- ZIO.service[UserDao]
      shipment <- ZIO.service[ShipmentDao]
      subscription <- ZIO.service[SubscriptionDao]
      scheduleStatus <- ZIO.service[ScheduleStatusDao]
      vesselService <- ZIO.service[VesselService]
      fileSourceService <- ZIO.service[FileSourceService]
    } yield SubscribeVesselServiceLive(user, shipment, subscription, scheduleStatus, vesselService, fileSourceService)
  }
}