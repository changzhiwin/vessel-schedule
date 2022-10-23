package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

case class FixScheduleStream(
  checkExpiredPipeline: CheckExpiredPipeline,
  fetchNewsPipeline: FetchNewsPipeline,
  notifySink: NotifySink
) {

  def start(period: Duration) = 
    ZStream
      .fromSchedule(Schedule.fixed(period))
      .tap(x => Console.printLine(s"Pipeline, before checkExpired: $x"))
      .via(checkExpiredPipeline.transform(period))
      .via(fetchNewsPipeline.transform())
      .run(notifySink.consume)
      .fork
}

object FixScheduleStream {

  val layer = ZLayer.fromFunction(FixScheduleStream.apply _)

  def start(period: Duration) = ZIO.serviceWithZIO[FixScheduleStream](_.start(period))

  def example(p: Long) =
    ZStream
      .fromSchedule(Schedule.fixed(p.seconds))
      .via(ZPipeline.map[Long, String](s => s"${s + 100}"))
      .via(ZPipeline.map[String, String](s => s"${s}-abc"))
      .run(
        ZSink.foreach { (str: String) => Console.printLine(s"Sink to: ${str}") }
      )
      .fork
}

