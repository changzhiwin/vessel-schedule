package zio.doreal.vessel.services

//import scala.concurrent.duration._

import zio._
import zio.doreal.vessel.dao.ShipmentDao

trait SchedulePullService {

  def loop(): ZIO[Any, Throwable, Unit]
}

object SchedulePullService {
  def loop(): ZIO[SchedulePullService, Throwable, Unit] = 
    ZIO.serviceWithZIO[SchedulePullService](_.loop())
}

case class SchedulePullServiceLive(pullNewsService: PullNewsService, shipmentDao: ShipmentDao) extends SchedulePullService {

  override def loop(): ZIO[Any, Throwable, Unit] = for {
    idle <- pullNewsService.isIdle().debug("Idle: ")
    _ <- ZIO.when(idle) {
      for {
        shipmentList <- shipmentDao.findExpiredUpdate(10.seconds) //(10.minutes)
        ids <- ZIO.succeed(shipmentList.map(_.id)).debug("Expired List: ")
        _ <- pullNewsService.accept(ids)
      } yield ()
    }
  } yield ()
}

object SchedulePullServiceLive {

  val live: ZLayer[PullNewsService with ShipmentDao, Nothing, SchedulePullService] = ZLayer {
    for {
      pullNews <- ZIO.service[PullNewsService]
      shipment <- ZIO.service[ShipmentDao]
    } yield SchedulePullServiceLive(pullNews, shipment)
  }
}