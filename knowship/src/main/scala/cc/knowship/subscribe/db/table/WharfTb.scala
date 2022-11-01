package cc.knowship.subscribe.db.table

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

import zio._
import io.getquill.context.qzio.ImplicitSyntax._

import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.model.Wharf

trait WharfTb {

  def create(bareWharf: Wharf): Task[Wharf]

  def findByCode(code: String): Task[Wharf]

  def all: Task[List[Wharf]]

  def get(id: UUID): Task[Wharf]

  def delete(id: UUID): Task[Unit]

}

final case class WharfTbLive(
  dataSource: DataSource
) extends WharfTb {
  
  import QuillContext._
  implicit val env = Implicit(dataSource)

  //def create(name: String, code: String, website: String): Task[Wharf] = for {
  def create(bareWharf: Wharf): Task[Wharf] = for {
    id     <- Random.nextUUID
    now    <- Clock.currentTime(TimeUnit.MILLISECONDS)

    wharf = bareWharf.copy(id = id, createAt = now)
    _ <- run(query[Wharf].insertValue(lift(wharf))).implicitly
  } yield wharf

  def findByCode(code: String): Task[Wharf] = 
    run(
      query[Wharf]
        .filter(s => s.code == lift(code))
    )
    .map(_.headOption)
    .someOrFail(DbRecordNotFound(s"Wharf#code ${code}"))
    .implicitly

  def all: Task[List[Wharf]] = run(query[Wharf]).implicitly

  def get(id: UUID): Task[Wharf] = 
    run(query[Wharf].filter(_.id == lift(id)))
      .map(_.headOption)
      .someOrFail(DbRecordNotFound(s"Wharf#id ${id.toString}"))
      .implicitly

  def delete(id: UUID): Task[Unit] = 
    run(query[Wharf].filter(_.id == lift(id)).delete).implicitly *> ZIO.unit

}

object WharfTbLive {

  val layer = ZLayer.fromFunction(WharfTbLive.apply _)
}