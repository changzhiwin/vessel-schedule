package cc.knowship.subscribe.db.table

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

import zio._
import io.getquill.context.qzio.ImplicitSyntax._

import cc.knowship.subscribe.SubscribeException._
import cc.knowship.subscribe.db.QuillContext
import cc.knowship.subscribe.db.model.Subscription

trait SubscriptionTb {

  def create(subscriberId: UUID, voyageId: UUID, infos: String): Task[Subscription]

  def update(id: UUID, infos: Option[String] = None): Task[Long]

  def updateOrCreate(subscriberId: UUID, voyageId: UUID, infos: String): Task[Subscription]

  def get(id: UUID): Task[Subscription]

  def delete(id: UUID): Task[Unit]

  def nonVoyageExist(voyageId: UUID): Task[Boolean]

  def findByVoyage(voyageId: UUID): Task[List[Subscription]]

  // def findByExpiredNotify(timestamp: Long): Task[List[Subscription]]
}

final case class SubscriptionTbLive(
  dataSource: DataSource
) extends SubscriptionTb { self =>
  
  import QuillContext._
  implicit val env = Implicit(dataSource)

  def create(subscriberId: UUID, voyageId: UUID, infos: String): Task[Subscription] = for {
    id  <- Random.nextUUID
    now <- Clock.currentTime(TimeUnit.MILLISECONDS)

    subscription = Subscription(id, subscriberId, voyageId, infos, now, now)
    _   <- run(query[Subscription].insertValue(lift(subscription))).implicitly
  } yield subscription

  def update(id: UUID, infos: Option[String] = None): Task[Long] = for {
    now <- Clock.currentTime(TimeUnit.MILLISECONDS)
    _   <- run(dynamicQuery[Subscription].filter(_.id == lift(id)).update(setValue(_.notifyAt, now), setOpt(_.infos, infos))).implicitly
  } yield now

  def updateOrCreate(subscriberId: UUID, voyageId: UUID, infos: String): Task[Subscription] = for {
    recordOpt    <- run(query[Subscription].filter(s => s.subscriberId == lift(subscriberId) && s.voyageId == lift(voyageId))).map(_.headOption).implicitly
    subscription <- ZIO.fromOption(recordOpt).foldZIO(
      notFound => self.create(subscriberId, voyageId, infos),
      valFound => self.update(valFound.id, Some(infos)).map(ts => valFound.copy(infos = infos, notifyAt = ts))
    )
  } yield subscription

  def get(id: UUID): Task[Subscription] =
    run(
      query[Subscription]
        .filter(s => s.id == lift(id))
    )
    .map(_.headOption)
    .someOrFail(DbRecordNotFound(s"Subscription#id ${id}"))
    .implicitly

  def delete(id: UUID): Task[Unit] = 
    run(
      query[Subscription]
        .filter(s => s.id == lift(id))
        .delete
    ).implicitly *> ZIO.unit

  def nonVoyageExist(voyageId: UUID): Task[Boolean] =
    self.findByVoyage(voyageId).map(_.isEmpty)

  def findByVoyage(voyageId: UUID): Task[List[Subscription]] =
    run(
      query[Subscription].filter(s => s.voyageId == lift(voyageId)) 
    ).implicitly 
}

object SubscriptionTbLive {

  val layer = ZLayer.fromFunction(SubscriptionTbLive.apply _)
}