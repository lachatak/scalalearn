import sbt._

lazy val commonSettings = Seq(
  name := "taglessfinal",
  organization := "org.kaloz",
  version := "1.0.0",
  scalaVersion := "2.12.4",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.bintrayIvyRepo("scalameta", "maven")
  ),
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),

  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-Ypartial-unification",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-Xplugin-require:macroparadise"
  )
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.0.1",
      "io.monix" %% "monix" % "3.0.0-M3",
      "com.typesafe.akka" %% "akka-http" % "10.0.11",
      "com.typesafe.akka" %% "akka-http-core" % "10.0.11",
      "de.heikoseeberger" %% "akka-http-json4s" % "1.18.1",
      "org.json4s" %% "json4s-core" % "3.5.0",
      "org.json4s" %% "json4s-jackson" % "3.5.0",
      "org.scalameta" %% "scalameta" % "2.1.5",

      "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11" % Test,
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.scalamock" %% "scalamock" % "4.0.0" % Test
    )
  )

