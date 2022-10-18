package zio.doreal.vessel

import zio._
import zio.config._
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfigSource

final case class AppConfig(bindPort: Int, notifyUrl: String, fetchPeriod: Int, syncPeriod: Int)

object AppConfig {

  /*
  val desc: ConfigDescriptor[AppConfig] =
    (int("bind-port") zip string("notify-url") zip int("fetch-period" zip int("sync-period")).optional).to[AppConfig]
  */

  val live: ZLayer[Any, ReadError[String], AppConfig] =
    ZLayer {
      read {
        descriptor[AppConfig].from(
          TypesafeConfigSource.fromResourcePath
            .at(PropertyTreePath.$("AppConfig"))
        )
      }
    }
 }