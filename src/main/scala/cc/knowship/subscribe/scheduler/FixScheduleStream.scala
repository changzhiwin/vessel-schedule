package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

case class FixScheduleStream(
  checkExpiredPipeline: CheckExpiredPipeline,
  fetchNewsPipeline: FetchNewsPipeline,
  notifySink: NotifySink
) {

  /**
    * 这个参数只是，调度的最小颗粒度；具体过期逻辑每个码头可以定制
    * @param period
    */
  def start(period: Duration) = 
    ZStream
      .fromSchedule(Schedule.fixed(period))
      .tap(x => ZIO.debug(s"Pipeline, it's $x times"))
      .via(checkExpiredPipeline.transform())
      .tap(x => ZIO.debug(x))
      .via(fetchNewsPipeline.transform())
      .onError(e => ZIO.log(s"Stream failed: ${e}"))
      .retry(Schedule.fibonacci(10.seconds))
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

