package zio.doreal.vessel.services

import zio._
import zio.doreal.vessel.dao.ShipmentDao

trait ScheduleFetchService {

  def loop(duration: Duration): ZIO[Any, Throwable, Unit]
}

object ScheduleFetchService {
  def loop(duration: Duration): ZIO[ScheduleFetchService, Throwable, Unit] = 
    ZIO.serviceWithZIO[ScheduleFetchService](_.loop(duration))
}

case class ScheduleFetchServiceLive(fetchNewsService: FetchNewsService, shipmentDao: ShipmentDao) extends ScheduleFetchService {

  override def loop(duration: Duration): ZIO[Any, Throwable, Unit] = for {
    idle <- fetchNewsService.isIdle().debug("Idle: ")
    _ <- ZIO.when(idle) {
      for {
        shipmentList <- shipmentDao.findExpiredUpdate(duration)
        ids <- ZIO.succeed(shipmentList.map(_.id)).debug("Expired List: ")
        _ <- fetchNewsService.accept(ids)
      } yield ()
    }
  } yield ()
}

object ScheduleFetchServiceLive {

  val live: ZLayer[FetchNewsService with ShipmentDao, Nothing, ScheduleFetchService] = ZLayer {
    for {
      fetchNews <- ZIO.service[FetchNewsService]
      shipment <- ZIO.service[ShipmentDao]
    } yield ScheduleFetchServiceLive(fetchNews, shipment)
  }
}