scalaVersion := "2.13.8"
organization := "zio.doreal"
name := "vessel"

val ZIOVersion = "2.0.2"
val ZIOHttpVersion = "2.0.0-RC11"

resolvers ++= Seq(
    "Scala-Tools Maven2 Snapshots Repository" at "https://scala-tools.org/repo-snapshots"
)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % ZIOVersion, 
  //"dev.zio" %% "zio-streams" % ZIOVersion, 
  "io.d11" %% "zhttp" % ZIOHttpVersion,
  //"dev.zio" %% "zio-http" % ZIOHttpVersion,
  //"dev.zio" %% "zio-macros" % ZIOVersion,
)

// Enable macro expansion, 
// from https://zio.dev/reference/service-pattern/generating-accessor-methods-using-macros
scalacOptions += "-Ymacro-annotations"