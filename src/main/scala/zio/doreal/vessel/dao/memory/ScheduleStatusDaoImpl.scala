package zio.doreal.vessel.dao.memory

import zio._
import zio.doreal.vessel.entities.{ScheduleStatus}
import zio.doreal.vessel.dao.ScheduleStatusDao

case class ScheduleStatusDaoImpl(repo: Ref[List[ScheduleStatus]]) extends ScheduleStatusDao {

  def save(scheduleStatusWithoutId: ScheduleStatus): ZIO[Any, Throwable, String] = for {
    id <- Random.nextUUID.map(_.toString())
    _ <- repo.update(_.appended(scheduleStatusWithoutId.copy(id = id)))
  } yield id

  def findById(id: String): ZIO[Any, Throwable, ScheduleStatus] = for {
    table <- repo.get
    record <- table.find(row => row.id == id) match {
      case Some(r) => ZIO.succeed(r)
      case None    => ZIO.fail(new java.lang.Throwable(s"No such ScheduleStatus with id [${id}]"))
    }
  } yield record

  def delete(id: String): ZIO[Any, Throwable, Boolean] = for {
    _ <- repo.update(_.filter(row => (row.id != id)))
  } yield true
}

object ScheduleStatusDaoImpl {
  def live: ULayer[ScheduleStatusDao] =
    ZLayer {
      Ref.make(List.empty[ScheduleStatus]).map(ScheduleStatusDaoImpl.apply)
    }
}