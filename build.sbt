ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "zio.doreal"
// ThisBuild / maintainer := "changzhiwin@gmail.com"

val ZIOVersion = "2.0.2"
val ZIOHttpVersion = "2.0.0-RC11+50-7870fdce+20220919-2120-SNAPSHOT"
val ZIOConfig = "3.0.2"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % ZIOVersion, 
  "dev.zio" %% "zio-json" % "0.3.0",
  "dev.zio" %% "zio-http" % ZIOHttpVersion,
  "dev.zio" %% "zio-config" % ZIOConfig,
  "dev.zio" %% "zio-config-typesafe" % ZIOConfig,
  "dev.zio" %% "zio-config-magnolia" % ZIOConfig,
  //"dev.zio" %% "zio-streams" % ZIOVersion,
  //"dev.zio" %% "zio-macros" % ZIOVersion,
)

// Enable macro expansion, 
// from https://zio.dev/reference/service-pattern/generating-accessor-methods-using-macros
scalacOptions ++= Seq(
  "-Ymacro-annotations",
  "-deprecation"
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "vessel-schedule",
  )