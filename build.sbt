scalaVersion := "2.13.8"
organization := "zio.doreal"
name := "vessel"

val ZIOVersion = "2.0.2"
val ZIOHttpVersion = "2.0.0-RC11+50-7870fdce+20220919-2120-SNAPSHOT"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % ZIOVersion, 
  "dev.zio" %% "zio-json" % "0.3.0",
  "dev.zio" %% "zio-http" % ZIOHttpVersion,
  //"dev.zio" %% "zio-streams" % ZIOVersion,
  //"dev.zio" %% "zio-macros" % ZIOVersion,
)

// Enable macro expansion, 
// from https://zio.dev/reference/service-pattern/generating-accessor-methods-using-macros
scalacOptions += "-Ymacro-annotations"