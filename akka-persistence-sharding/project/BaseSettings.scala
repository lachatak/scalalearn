import sbt.Keys._
import sbt._

object BaseSettings {

  lazy val defaultSettings =
    Seq(
      organization := "org.kaloz.persistence",
      description := "Akka Persistence Sharding Test",
      version := "1.0.0",
      scalaVersion := "2.12.3",
      licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      homepage := Some(url("http://kaloz.org")),
      crossPaths := false,
//      scalaOrganization in ThisBuild := "org.typelevel",
      scalacOptions := Seq(
        "-encoding", "utf8",
        "-feature",
        "-unchecked",
        "-deprecation",
        "-target:jvm-1.8",
        "-language:postfixOps",
        "-language:implicitConversions",
        "-Ypartial-unification"
      ),
      javacOptions := Seq(
        "-encoding", "utf8",
        "-Xlint:unchecked",
        "-Xlint:deprecation"
      ),
      shellPrompt := { s => "[" + scala.Console.BLUE + Project.extract(s).currentProject.id + scala.Console.RESET + "] $ " }
    ) ++
      Resolvers.defaultSettings
}