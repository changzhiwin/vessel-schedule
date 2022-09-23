package zio.doreal.vessel.dao.memory

import zio._
import zio.doreal.vessel.entities.User
import zio.doreal.vessel.dao.UserDao

case class UserDaoImpl(repo: Ref[List[User]]) extends UserDao {

  def findOrCreate(userWithOutId: User): Task[User] = for {
    table <- repo.get
    user <- table.find(row => row.equalWithAttributes(userWithOutId)) match {
      case Some(user) => ZIO.succeed(user)
      case None => for {
        id <- Random.nextUUID.map(_.toString())
        newUser <- repo.modify(t => { val rd = userWithOutId.copy(id = id); (rd, t.appended(rd))})
      } yield newUser
    }
  } yield user

  def findById(id: String): Task[User] = for {
    table <- repo.get
    user <- table.find(row => row.id == id) match {
      case Some(u) => ZIO.succeed(u)
      case None    => ZIO.fail(new java.lang.Throwable(s"No such user with id [${id}]"))
    }
  } yield user

  def getAll(): Task[List[User]] = repo.get
}

object UserDaoImpl {

  def live: ULayer[UserDao] =
    ZLayer {
      Ref.make(List.empty[User]).map(UserDaoImpl.apply)
    }
}