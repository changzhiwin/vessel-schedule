package cc.knowship.subscribe

import zio._
import zio.config._
import ConfigDescriptor._

final case class SubscribeConfig(bindPort: Int, loopUnit: Duration, notifyUrl: String)

object SubscribeConfig {
  val configuration: ConfigDescriptor[SubscribeConfig] =
    (int("bindPort") zip zioDuration("loopUnit") zip string("notifyUrl")).to[SubscribeConfig]
}

final case class WharfConfig(voyUrl: String, scheUrl: String)

object WharfConfig {
  val configuration: ConfigDescriptor[WharfConfig] =
    (string("voyUrl") zip string("scheUrl")).to[WharfConfig]
}

final case class AppConfig(subscribe: SubscribeConfig, cmg: WharfConfig, npedi: WharfConfig)

object AppConfig {

  val subscribe = SubscribeConfig.configuration
  val wharf = WharfConfig.configuration

  val configuration: ConfigDescriptor[AppConfig] =
    (
      nested("subscribe")(subscribe) zip
      nested("cmg")(wharf) zip
      nested("npedi")(wharf)
    ).to[AppConfig]

  val layer = ZConfig.fromPropertiesFile(
    "./src/main/resources/application.properties", 
    AppConfig.configuration, 
    Some('.'),
    Some(',')
  )
}