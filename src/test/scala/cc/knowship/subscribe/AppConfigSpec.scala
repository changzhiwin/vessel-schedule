package cc.knowship.subscribe

import zio._
import zio.config._
import zio.test._
//import zio.test.Assertion._
//import java.io.IOException

import ConfigDescriptor._

final case class ApplicationConfig(bridgeIp: String, userName: String)

object ApplicationConfig {
  val configuration: ConfigDescriptor[ApplicationConfig] =
    ((string("bridgeIp")) zip string("username")).to[ApplicationConfig]
}

// Ref:https://github.com/zio/zio-config/blob/master/examples/shared/src/main/scala/zio/config/examples/NestedConfigExample.scala
case class Database(url: String, port: Int)
object Database {
  val configuration: ConfigDescriptor[Database] =
    (string("connection") zip int("port")).to[Database]
}

case class AwsConfig(c1: Database, c2: Database, c3: String)
object AwsConfig {
  val database = Database.configuration
  val configuration: ConfigDescriptor[AwsConfig] =
    (
      nested("south")(database) ?? "South details" zip
      nested("east")(database) ?? "East details" zip
      nested("appName")(string)
    ).to[AwsConfig]
}

object AppConfigSpec extends ZIOSpecDefault {

  def spec = suite("all suites")(
    suite("AppConfigSpecSpec")(
      test("fromProperties have key= bridgeIp, value = 127.0.0.2") {
        for {
          config      <- ZIO.service[ApplicationConfig]
        } yield assertTrue(config.bridgeIp == "127.0.0.2")
      }
    ).provideShared(
      ZConfig.fromPropertiesFile(
        "./src/test/resources/application.properties", 
        ApplicationConfig.configuration, 
        Some('.'),
        Some(','))
    ),
    
    suite("AwsConfig & Database")(
      test("AwsConfig have key= appName, value = myVessleApp") {
        for {
          config <- ZIO.service[AwsConfig]
        } yield assertTrue(config.c3 == "myVessleApp")
      },
      test("AwsConfig#Database have key= east.connection, value = xyy.com") {
        for {
          config <- ZIO.service[AwsConfig]
        } yield assertTrue(config.c2.url == "xyy.com")
      }
    ).provideShared(
      ZConfig.fromPropertiesFile(
        "./src/test/resources/application1.properties", 
        AwsConfig.configuration, 
        Some('.'),
        Some(','))
    ),
  )
}
