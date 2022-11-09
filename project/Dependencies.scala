import sbt.Keys.scalaVersion
import sbt._

object Dependencies {
  val ZioVersion   = "2.0.3"
  val ZioConfig    = "3.0.2"
  val ZioHttp      = "2.0.0-RC11+159-5abadb95+20221109-1839-SNAPSHOT" //"2.0.0-RC11+119-0cd216c3+20221024-1727-SNAPSHOT" //"2.0.0-RC11+50-7870fdce+20220919-2120-SNAPSHOT"
  val ZioJson      = "0.3.0"
  val ZioLogging   = "2.1.3"
  val ZioQuillJDBC = "4.6.0"
  val SqliteJDBC   = "3.28.0"
  val Mysql        = "8.0.17"

  val zio                   = "dev.zio"     %% "zio"                 % ZioVersion
  val `zio-streams`         = "dev.zio"     %% "zio-streams"         % ZioVersion
  val `zio-json`            = "dev.zio"     %% "zio-json"            % ZioJson
  val `zio-http`            = "dev.zio"     %% "zio-http"            % ZioHttp
  val `zio-test`            = "dev.zio"     %% "zio-test"            % ZioVersion % "test"
  val `zio-test-sbt`        = "dev.zio"     %% "zio-test-sbt"        % ZioVersion % "test"
  val `zio-test-magnolia`   = "dev.zio"     %% "zio-test-magnolia"   % ZioVersion % "test"

  val `zio-config`          = "dev.zio"     %% "zio-config"          % ZioConfig
  val `zio-config-typesafe` = "dev.zio"     %% "zio-config-typesafe" % ZioConfig

  val `zio-logging`         = "dev.zio"     %% "zio-logging"         % ZioLogging

  val `quill-jdbc-zio`      = "io.getquill" %% "quill-jdbc-zio"      % ZioQuillJDBC
  val `sqlite-jdbc`         = "org.xerial"   % "sqlite-jdbc"         % SqliteJDBC
  val `mysql-connector-java` = "mysql"       % "mysql-connector-java" % Mysql
}