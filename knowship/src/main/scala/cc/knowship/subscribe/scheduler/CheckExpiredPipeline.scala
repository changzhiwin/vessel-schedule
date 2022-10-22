package cc.knowship.subscribe.scheduler

import java.util.concurrent.TimeUnit

import zio._
import zio.stream._

import cc.knowship.subscribe.db.model.Voyage
import cc.knowship.subscribe.db.table.VoyageTb

/**
  * 最后更新的时间滞后，给定的时间周期，则需要获取最新的状态
  * 有点问题：新的订阅会刷新updateAt和createAt，导致老的订阅用户不能得到通知
  * 解决办法：合并通过voyage查找 和 subscription中notifyAt超时的
  * @param voyageTb
  */
case class CheckExpiredPipeline(voyageTb: VoyageTb) {

  def handler(period: Duration) = ZPipeline.mapZIO[Int, Chunk[Voyage]] { times =>
    for {
      now     <- Clock.currentTime(TimeUnit.MILLISECONDS)
      voyages <- voyageTb.findByExpiredPeriod(now - period.toMillis())
    } yield Chunk.from(voyages)
  } >>> ZPipeline.mapChunks[Chunk[Voyage], Voyage](_.flatten)

}

object CheckExpiredPipeline {
  val layer = ZLayer.fromFunction(CheckExpiredPipeline.apply _)
}

