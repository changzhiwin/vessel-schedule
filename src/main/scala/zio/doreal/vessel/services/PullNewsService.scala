package zio.doreal.vessel.services

import zio._
import zio.doreal.vessel.dao._
import zio.doreal.vessel.wharf.VesselService

trait PullNewsService {

  def isIdle(): ZIO[Any, Nothing, Boolean]

  def accept(shipmentIds: List[String]): ZIO[Any, Throwable, Int]

  def start(): ZIO[Any, Throwable, Unit]

  def stop(): ZIO[Any, Throwable, Unit]
}

object PullNewsService {

  def start(): ZIO[PullNewsService, Throwable, Unit] = 
    ZIO.serviceWithZIO[PullNewsService](_.start())

  case class PullNewsServiceLive(queue: Queue[String], 
      vesselService: VesselService, 
      publishSubscriptService: PublishSubscriptService,
      shipmentDao: ShipmentDao, 
      scheduleStatusDao: ScheduleStatusDao) extends PullNewsService {

    def isIdle(): ZIO[Any, Nothing, Boolean] = queue.isFull.map(x => !x)

    def accept(shipmentIds: List[String]): ZIO[Any, Throwable, Int] = queue.offerAll(shipmentIds).map(ck => ck.size)

    def start(): ZIO[Any, Throwable, Unit] = ZIO.whileLoop(true) {
      for {
        shipmentId <- queue.take.debug("Queue take: ")
        shipment   <- shipmentDao.findById(shipmentId)
        status     <- scheduleStatusDao.findById(shipment.scheduleStatusId)
        scheduleStatusReply <- vesselService.xxx
        freshResult         <- scheduleStatusReply.code match {
          case 0 => for {
            scheduleStatusId <- scheduleStatusDao.update(status.id, scheduleStatusReply.status.get)
            _                <- shipmentDao.updateScheduleStatus(shipment.id, scheduleStatusId)
            _                <- publishSubscriptService.notify(shipment.id, scheduleStatusId)
          }
          case _ => ZIO.log(s"Remote Fetch Error: ${scheduleStatusReply.message}")
        }
      } yield ()
    } { _ => }

    def stop(): ZIO[Any, Throwable, Unit] = queue.shutdown
  }

  val live: ZLayer[VesselService with ShipmentDao with ScheduleStatusDao, Nothing, PullNewsService] = ZLayer {
    for {
      queue <- Queue.dropping[String](100)
      vesselService <- ZIO.service[VesselService]
      publishSubscriptService <- ZIO.service[PublishSubscriptService]
      shipment <- ZIO.service[ShipmentDao]
      status <- ZIO.service[ScheduleStatusDao]
    } yield PullNewsServiceLive(queue, vesselService, publishSubscriptService, shipment, status)
  }
}