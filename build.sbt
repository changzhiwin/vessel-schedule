import BuildHelper._
import Dependencies._

lazy val root = (project in file("."))
  .settings(stdSettings("root"))
  .settings(publishSetting(false))
  .settings(runSettings(Debug.Main))
  .settings(
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      `zio`, `zio-test`, `zio-test-sbt`, 
      `zio-http`, `zio-json`, `zio-streams`,
      `quill-jdbc-zio`, `sqlite-jdbc`,
      `zio-config`,
    ),
  )