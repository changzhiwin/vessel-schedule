package zio.doreal.vessel.dao

import zio._
import zio.doreal.vessel.entities.{User}

trait UserDao {

  def findOrCreate(userWithOutId: User): Task[User]

  def findById(id: String): Task[User]

  def getAll(): Task[List[User]]

  def init(users: List[User]): Task[Boolean]
}