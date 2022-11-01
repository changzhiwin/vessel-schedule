package cc.knowship.subscribe.scheduler

import java.util.concurrent.TimeUnit
import scala.util.Random

import zio._
import zio.stream._

import cc.knowship.subscribe.util.TimeDateUtils
import cc.knowship.subscribe.db.model.Voyage
import cc.knowship.subscribe.db.table.{VoyageTb, WharfTb}

/**
  * 最后更新的时间滞后，给定的时间周期，则需要获取最新的状态
  * 有点问题：新的订阅会刷新updateAt和createAt，导致老的订阅用户不能得到通知
  * 解决办法：合并voyage#updateAt 和 subscription#notifyAt 过期的内容。这个方法有问题，导致没有真正变化的频繁更新
  * 办法2:   新订阅，订阅已存在的航次时，返回本地的状态，不请求远端服务，解决。
  * @param voyageTb
  */
case class CheckExpiredPipeline(wharfTb: WharfTb, voyageTb: VoyageTb) {

  def transform() = 
    ZPipeline.mapZIO[Any, Throwable, Long, Chunk[Voyage]](t => filterExpiredSubscription(t)) >>> 
    ZPipeline.mapChunks[Chunk[Voyage], Voyage](_.flatten)

  // 优化：添加一些策略
  // 1，只在工作时间执行流水线
  // 2，不同码头失效周期间隔不同
  private def filterExpiredSubscription(times: Long): Task[Chunk[Voyage]] = for {
      now     <- Clock.currentTime(TimeUnit.MILLISECONDS)
      wharfs  <- wharfTb.all
      voyList <- ZIO.foreach(wharfs) { whf =>
        if (TimeDateUtils.isWorkingTime(now, whf.workStart, whf.workEnd)) {
          voyageTb.findUpdateAtExpiredByWharf(whf.id, now - whf.period)
        } else {
          ZIO.succeed(List.empty[Voyage])
        }
      }
  } yield Chunk.from(Random.shuffle(voyList.flatten))  // shuffle打乱，使不同码头间隔开来
}

object CheckExpiredPipeline {
  val layer = ZLayer.fromFunction(CheckExpiredPipeline.apply _)
}

