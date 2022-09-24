package zio.doreal.vessel.services

import zio._

trait PullNewsService {

  def isIdle(): ZIO[Any, Nothing, Boolean]

  def accept(shipmentIds: List[String]): ZIO[Any, Throwable, Int]

  def start(): ZIO[Any, Throwable, Unit]

  def stop(): ZIO[Any, Throwable, Unit]
}

object PullNewsService {

  case class PullNewsServiceLive(queue: Queue[String]) extends PullNewsService {

    def isIdle(): ZIO[Any, Nothing, Boolean] = queue.isEmpty

    def accept(shipmentIds: List[String]): ZIO[Any, Throwable, Int] = queue.offerAll(shipmentIds).debug("Queue Offer: ").map(ck => ck.size)

    def start(): ZIO[Any, Throwable, Unit] = ZIO.whileLoop(true) {
      for {
        shipmentId <- queue.take
        _ <- ZIO.log(shipmentId)
      } yield ()
    } { _ => }

    def stop(): ZIO[Any, Throwable, Unit] = queue.shutdown
  }

  object PullNewsServiceLive {
    val live: ZLayer[Any, Nothing, PullNewsService] = ZLayer {
      for {
        queue <- Queue.dropping[String](100)
      } yield PullNewsServiceLive(queue)
    }
  }
}