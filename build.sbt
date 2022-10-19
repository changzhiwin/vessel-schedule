import BuildHelper._
import Dependencies._

lazy val root = (project in file("."))
  .settings(stdSettings("root"))
  //.settings(publishSetting(false))
  .aggregate(
    knowship,
  )

lazy val vessel = (project in file("vessel"))
  .enablePlugins(JavaAppPackaging)
  .settings(stdSettings("vessel"))
  .settings(publishSetting(false))
  .settings(
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      `zio`, `zio-test`, `zio-test-sbt`, `zio-test-magnolia`,
      `zio-config`, `zio-config-typesafe`, `zio-config-magnolia`,
      `zio-http`, `zio-json`,
    ),
  )

lazy val knowship = (project in file("knowship"))
  .settings(stdSettings("knowship"))
  .settings(publishSetting(false))
  .settings(runSettings(Debug.Main))
  .settings(
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      `zio`, `zio-test`, `zio-test-sbt`, 
      `zio-http`, `zio-json`,
      `quill-jdbc-zio`, `sqlite-jdbc`,
    ),
  )