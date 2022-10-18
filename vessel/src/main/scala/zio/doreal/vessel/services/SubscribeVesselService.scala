package zio.doreal.vessel.services

import zio._
import zio.http._
import zio.json._

import zio.doreal.vessel.controls.vo.SubscribeParams
import zio.doreal.vessel.dao._
import zio.doreal.vessel.services.FileSourceService
import zio.doreal.vessel.wharf.VesselService
import zio.doreal.vessel.entities.ScheduleStatus
import zio.doreal.vessel.basic.format.TypedContent
import zio.doreal.vessel.services.format._

trait SubscribeVesselService {

  def subscribe(params: SubscribeParams): ZIO[Any, Throwable, TypedContent]

  def unsubscribe(params: SubscribeParams): ZIO[Any, Throwable, TypedContent] 

  def admin(params: SubscribeParams): ZIO[Any, Throwable, TypedContent]
}

case class SubscribeVesselServiceLive(
    userDao: UserDao, 
    shipmentDao: ShipmentDao, 
    subscriptionDao: SubscriptionDao,
    scheduleStatusDao: ScheduleStatusDao,
    vesselService: VesselService,
    fileSourceService: FileSourceService) extends SubscribeVesselService {

 def subscribe(params: SubscribeParams): ZIO[Any, Throwable, TypedContent] = for {
    user        <- userDao.findOrCreate(params.getUserWithOutId)
    shipmentOpt <- shipmentDao.findByQueryKey(params.getQueryKey)
    content     <- shipmentOpt match {
      case Some(shipment) => {
        for {
          added  <- subscriptionDao.subscribe(user.id, shipment.id, params.extraInfo).debug("Subscribe: ")
          status <- scheduleStatusDao.findById(shipment.scheduleStatusId)
        } yield SubscribeContent(status = Some(status), extraInfo = Some(params.extraInfo))
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
              _                <- subscriptionDao.subscribe(user.id, shipmentId, params.extraInfo)
              status           <- scheduleStatusDao.findById(scheduleStatusId)
            } yield SubscribeContent(status = Some(status), extraInfo = Some(params.extraInfo))
            case _ => ZIO.succeed( SubscribeContent(message = scheduleStatusReply.message) )
          }
        } yield freshResult
      }
    }
    _    <- fileSourceService.needSync
  } yield content

  def unsubscribe(params: SubscribeParams): ZIO[Any, Throwable, TypedContent] = for {
    shipmentOpt <- shipmentDao.findByQueryKey(params.getQueryKey)
    content     <- shipmentOpt match {
      case Some(shipment) => for {
        user  <- userDao.findOrCreate(params.getUserWithOutId)
        added <- subscriptionDao.unsubscribe(user.id, shipment.id, params.extraInfo)
        // TODO: if added == 0, no need delete operation
        yes   <- subscriptionDao.noSubscriberExist(shipment.id)
        _     <- if (yes) scheduleStatusDao.delete(shipment.scheduleStatusId) *> shipmentDao.delete(shipment.id) else ZIO.unit
        tips  <- if (added == 0) ZIO.succeed("You don't have subscribe") else fileSourceService.needSync *> ZIO.succeed("Unsubscribe success")
      } yield UnSubscribeContent(message = tips, rawSubscribe = params.getSubscribeInfo)
      case None => ZIO.succeed( UnSubscribeContent(message = "Not exist", rawSubscribe = params.getSubscribeInfo) )
    }
  } yield content

  def admin(params: SubscribeParams): ZIO[Any, Throwable, TypedContent] = for {
    userList <- userDao.getAll()
    shipmentList <- shipmentDao.getAll()
    subscribeList <- subscriptionDao.getAll()
    detailList <- ZIO.foreach(subscribeList) { sub =>
      for {
        user     <- userDao.findById(sub.userId)
        shipment <- shipmentDao.findById(sub.shipmentId)
      } yield s"[${user.openId}] -> ${shipment.queryKey} -> ${sub.extraInfo}"
    }
  } yield AdminContent(userList = userList, shipmentList = shipmentList, subDetails = detailList)
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