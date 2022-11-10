import BuildHelper._
import Dependencies._

ThisBuild / version          := "2.0.3"
ThisBuild / organizationName := "knowship"

maintainer := "changzhiwin@gmail.com"

lazy val root = (project in file("."))
  //.enablePlugins(JavaAppPackaging)
  .enablePlugins(JavaServerAppPackaging)
  .settings(stdSettings("vessel-schedule"))
  .settings(publishSetting(false))
  .settings(runSettings(Debug.Main))
  .settings(
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      `zio`, `zio-test`, `zio-test-sbt`, 
      `zio-http`, `zio-json`, `zio-streams`,
      `quill-jdbc-zio`, `sqlite-jdbc`,
      `zio-config`, `zio-config-typesafe`,
      `zio-logging`
    ),
  )