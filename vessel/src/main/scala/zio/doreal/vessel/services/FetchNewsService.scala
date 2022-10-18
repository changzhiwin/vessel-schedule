package zio.doreal.vessel.services

import zio._
import zio.doreal.vessel.dao._
import zio.doreal.vessel.wharf.VesselService

trait FetchNewsService {

  def isIdle(): ZIO[Any, Nothing, Boolean]

  def accept(shipmentIds: List[String]): ZIO[Any, Throwable, Int]

  def start(): ZIO[Any, Throwable, Unit]

  def stop(): ZIO[Any, Throwable, Unit]
}

object FetchNewsService {

  def start(): ZIO[FetchNewsService, Throwable, Unit] = 
    ZIO.serviceWithZIO[FetchNewsService](_.start())

  case class FetchNewsServiceLive(queue: Queue[String], 
      vesselService: VesselService, 
      publishSubscriptService: PublishSubscriptService,
      fileSourceService: FileSourceService,
      shipmentDao: ShipmentDao, 
      scheduleStatusDao: ScheduleStatusDao) extends FetchNewsService {

    def isIdle(): ZIO[Any, Nothing, Boolean] = queue.isFull.map(x => !x)

    def accept(shipmentIds: List[String]): ZIO[Any, Throwable, Int] = queue.offerAll(shipmentIds).map(ck => ck.size)

    def start(): ZIO[Any, Throwable, Unit] = ZIO.whileLoop(true) {
      for {
        shipmentId <- queue.take.debug("Fetch News Queue take: ")
        shipment   <- shipmentDao.findById(shipmentId)
        oldStatus  <- scheduleStatusDao.findById(shipment.scheduleStatusId)
        scheduleStatusReply <- vesselService.getScheduleStatusDirectly(shipment.vessel, shipment.voyage, shipment.wharf)
        _          <- scheduleStatusReply.code match {
          case 0 => {
            val newStatus = scheduleStatusReply.status.get
            val diffResult = oldStatus.isNotChangeOrEnd(newStatus)
            if (diffResult._1) {
              // Notice: don't active needSync, because have not real changed
              shipmentDao.updateFetchTime(shipment) *> ZIO.unit
            } else {
              for {
                statusId <- scheduleStatusDao.update(oldStatus.id, newStatus)
                _ <- shipmentDao.updateTimeAndDetail(shipment, statusId)
                _ <- publishSubscriptService.notify(shipment.id, statusId, diffResult._2)
                _ <- fileSourceService.needSync
              } yield ()
            }
          }
          case _ => ZIO.log(s"Remote Fetch Error: ${scheduleStatusReply.message}")
        }
      } yield ()
    } { _ => }

    def stop(): ZIO[Any, Throwable, Unit] = queue.shutdown
  }

  val live: ZLayer[VesselService with PublishSubscriptService with FileSourceService with ShipmentDao with ScheduleStatusDao, Nothing, FetchNewsService] = ZLayer {
    for {
      queue <- Queue.dropping[String](100)
      vesselService <- ZIO.service[VesselService]
      publishSubscriptService <- ZIO.service[PublishSubscriptService]
      fileSourceService <- ZIO.service[FileSourceService]
      shipment <- ZIO.service[ShipmentDao]
      status <- ZIO.service[ScheduleStatusDao]
    } yield FetchNewsServiceLive(queue, vesselService, publishSubscriptService, fileSourceService, shipment, status)
  }
}