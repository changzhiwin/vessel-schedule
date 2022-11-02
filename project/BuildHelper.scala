import sbt.Keys._
import sbt._

object BuildHelper extends ScalaSettings {
  val Scala212         = "2.12.16"
  val Scala213         = "2.13.8"
  val ScalaDotty       = "3.2.0"

  private val stdOptions = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-language:postfixOps",
  )

  def extraOptions(scalaVersion: String) =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((3, 0))  => scala3Settings
      case Some((2, 12)) => scala212Settings
      case Some((2, 13)) => scala213Settings
      case _             => Seq.empty
    }

  def publishSetting(publishArtifacts: Boolean) = {
    val publishSettings = Seq()
    val skipSettings    = Seq(
      publish / skip  := true,
      publishArtifact := false,
    )
    if (publishArtifacts) publishSettings else publishSettings ++ skipSettings
  }

  def stdSettings(prjName: String) = Seq(
    name                                   := s"$prjName",
    ThisBuild / crossScalaVersions         := Seq(Scala212, Scala213, ScalaDotty),
    ThisBuild / scalaVersion               := Scala213,
    ThisBuild / organization               := "cc.knowship",
    scalacOptions                          := stdOptions ++ extraOptions(scalaVersion.value),
    Test / parallelExecution               := true,
    ThisBuild / javaOptions                := Seq(
      s"-DZIOHttpLogLevel=${Debug.ZIOHttpLogLevel}",
    ),
    //ThisBuild / fork                       := true,
  )

  def runSettings(className: String = "cc.knowship.subscribe.MainApp") = Seq(
    fork                      := true,
    Compile / run / mainClass := Option(className),
  )

  def meta = Seq(
    ThisBuild / homepage   := Some(url("https://github.com/changzhiwin/vessel-schedule")),
    ThisBuild / scmInfo    :=
      Some(
        ScmInfo(url("https://github.com/changzhiwin/vessel-schedule"), "git@github.com:changzhiwin/vessel-schedule.git"),
      ),
    ThisBuild / developers := List(
      Developer(
        "changzhiwin",
        "Zhi Chang",
        "changzhiwin@gmail.com",
        new URL("https://github.com/changzhiwin"),
      ),
    ),
  )
}