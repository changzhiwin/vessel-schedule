package cc.knowship.subscribe.db.table

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

import zio._
import io.getquill.context.qzio.ImplicitSyntax._

import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.model.Subscriber

trait SubscriberTb {

  def create(openId: String, source: String, receiver: String, nickname: String): Task[Subscriber]

  def get(id: UUID): Task[Option[Subscriber]]

  def findByOpenId(openId: String, source: String): Task[Option[Subscriber]]

  def all: Task[List[Subscriber]]

  def delete(id: UUID): Task[Unit]
}

final case class SubscriberTbLive(
  dataSource: DataSource
) extends SubscriberTb {
  
  import QuillContext._
  implicit val env = Implicit(dataSource)

  def create(openId: String, source: String, receiver: String, nickname: String): Task[Subscriber] = for {
    id     <- Random.nextUUID
    now    <- Clock.currentTime(TimeUnit.MILLISECONDS)

    subscriber = Subscriber(id, openId, source, receiver, nickname, now, now)
    _ <- run(query[Subscriber].insertValue(lift(subscriber))).implicitly /*.returning(r => r) Not support by sqlite*/
  } yield subscriber

  def get(id: UUID): Task[Option[Subscriber]] = 
    run(
      query[Subscriber]
        .filter(s => s.id == lift(id))
    ).map(_.headOption).implicitly

  def findByOpenId(openId: String, source: String): Task[Option[Subscriber]] =
    run(
      query[Subscriber]
        .filter(s => s.openId == lift(openId) && s.source == lift(source))
    ).map(_.headOption).implicitly

  def all: Task[List[Subscriber]] = run(query[Subscriber]).implicitly

  def delete(id: UUID): Task[Unit] = 
    run(
      query[Subscriber]
        .filter(s => s.id == lift(id))
        .delete
    ).implicitly *> ZIO.unit
}

object SubscriberTbLive {

  val layer = ZLayer.fromFunction(SubscriberTbLive.apply _)
}