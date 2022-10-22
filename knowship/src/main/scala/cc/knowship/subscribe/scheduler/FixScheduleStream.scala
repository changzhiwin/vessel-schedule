package cc.knowship.subscribe.scheduler

import zio._
import zio.stream._

// 流式：PipeLine？ ZStream.fromSchedule
// flatMap[R1 <: R, E1 >: E, B](f: (A) => ZStream[R1, E1, B])(implicit trace: Trace): ZStream[R1, E1, B]
// 定时器(15分钟) -> 需要更新(wharf/vessel/voyage) -> 获取最新(http request, db update, need notify?) -> 通知

//case class FixScheduleStream() {}

object FixScheduleStream extends ZIOAppDefault{

  def streams(p: Long) = ZStream
              .fromSchedule(Schedule.fixed(p.seconds))
              .via(ZPipeline.map[Long, String](s => s"${s + 100}"))
              .via(ZPipeline.map[String, String](s => s"${s}-abc"))
              .run(
                ZSink.foreach { (str: String) => Console.printLine(s"Sink to: ${str}") }
              ).fork

  def run = for {
    _ <- Console.printLine("stream start...")
    _ <- streams(3)
    _ <- Console.printLine("stream end...") *> ZIO.never
  } yield ()
}

