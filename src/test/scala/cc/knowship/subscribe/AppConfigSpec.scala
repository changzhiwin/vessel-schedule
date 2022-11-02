package cc.knowship.subscribe

import zio._
import zio.config._
import zio.test._

// Ref:https://github.com/zio/zio-config/blob/master/examples/shared/src/main/scala/zio/config/examples/NestedConfigExample.scala

object AppConfigSpec extends ZIOSpecDefault {

  def spec = suite("all suites")(
    suite("AppConfigSpec")(
      test("AppConfig key(subscribe.bindPort) have Int(8088)") {
        for {
          config <- ZIO.service[AppConfig]
        } yield assertTrue(config.subscribe.bindPort == 8088)
      },
      test("AppConfig key(subscribe.loopUnit) have Duration(31.senconds)") {
        for {
          config <- ZIO.service[AppConfig]
        } yield assertTrue(config.subscribe.loopUnit == 31.seconds)
      },
      test("AppConfig have key(npedi.voyUrl) have empty String") {
        for {
          config <- ZIO.service[AppConfig]
        } yield assertTrue(config.npedi.voyUrl.length == 0)
      },
      test("AppConfig have key(npedi.scheUrl) have String(http://127.0.0.1:4427/retrieve/npedi)") {
        for {
          config <- ZIO.service[AppConfig]
        } yield assertTrue(config.npedi.scheUrl == "http://127.0.0.1:4427/retrieve/npedi")
      }
    ).provideShared(
      ZConfig.fromPropertiesFile(
        "./src/test/resources/application.properties", 
        AppConfig.configuration, 
        Some('.'),
        Some(','))
    )
  )
}