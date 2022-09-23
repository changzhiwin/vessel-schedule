package zio.doreal.vessel.dao

import zio._
import zio.doreal.vessel.entities.{ScheduleStatus}

trait ScheduleStatusDao {

  def save(scheduleStatusWithoutId: ScheduleStatus): ZIO[Any, Throwable, String]

  def findById(id: String): ZIO[Any, Throwable, ScheduleStatus]

  def delete(id: String): ZIO[Any, Throwable, Boolean]
}