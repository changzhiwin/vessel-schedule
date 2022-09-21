package zio.doreal.vessel.service

import zio._
import zio.doreal.vessel.models.{ScheduleStatus}

trait ScheduleStatusRepo {

  def register(statusWithoutId: ScheduleStatus): ZIO[Any, Throwable, String]

  def findById(id: String): ZIO[Any, Throwable, ScheduleStatus]

  def delete(id: String): ZIO[Any, Throwable, Boolean]
}

object ScheduleStatusRepo {

  def register(statusWithoutId: ScheduleStatus): ZIO[ScheduleStatusRepo, Throwable, String] =
    ZIO.serviceWithZIO[ScheduleStatusRepo](_.register(statusWithoutId))

  def findById(id: String): ZIO[ScheduleStatusRepo, Throwable, ScheduleStatus] =
    ZIO.serviceWithZIO[ScheduleStatusRepo](_.findById(id))

  def delete(id: String): ZIO[ScheduleStatusRepo, Throwable, Boolean] =
    ZIO.serviceWithZIO[ScheduleStatusRepo](_.delete(id))
}