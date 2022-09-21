package zio.doreal.vessel.service

import zio._
import zio.doreal.vessel.models.{User}

trait UserRepo {

  def register(email: String): Task[String]

  def findById(id: String): Task[User]

  def delete(id: String): Task[Boolean]
}

object UserRepo {

  def register(email: String): ZIO[UserRepo, Throwable, String] =
    ZIO.serviceWithZIO[UserRepo](_.register(email))

  def findById(id: String): ZIO[UserRepo, Throwable, User] =
    ZIO.serviceWithZIO[UserRepo](_.findById(id))

  def delete(id: String): ZIO[UserRepo, Throwable, Boolean] =
    ZIO.serviceWithZIO[UserRepo](_.delete(id))
}