package cc.knowship.subscribe.scheduler

import java.util.concurrent.TimeUnit

import zio._
import zio.stream._

import cc.knowship.subscribe.db.model.Voyage
import cc.knowship.subscribe.db.table.VoyageTb

/**
  * 最后更新的时间滞后，给定的时间周期，则需要获取最新的状态
  * 有点问题：新的订阅会刷新updateAt和createAt，导致老的订阅用户不能得到通知
  * 解决办法：合并voyage#updateAt 和 subscription#notifyAt 过期的内容。这个方法有问题，导致没有真正变化的频繁更新
  * 办法2:   新订阅，订阅已存在的航次时，返回本地的状态，不请求远端服务，TODO
  * @param voyageTb
  */
case class CheckExpiredPipeline(voyageTb: VoyageTb) {

  def transform(period: Duration) = ZPipeline.mapZIO[Any, Throwable, Long, Chunk[Voyage]](
    
    times => for {
      now     <- Clock.currentTime(TimeUnit.MILLISECONDS)
      voyages <- voyageTb.findByExpiredUpdate(now - period.toMillis())
    } yield Chunk.from(voyages)

  ) >>> ZPipeline.mapChunks[Chunk[Voyage], Voyage](_.flatten)

}

object CheckExpiredPipeline {
  val layer = ZLayer.fromFunction(CheckExpiredPipeline.apply _)
}

