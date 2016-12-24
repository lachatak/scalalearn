import sbt._

lazy val commonSettings = Seq(
  name := "freemonad",
  organization := "org.kaloz",
  version := "1.0.0",
  scalaVersion := "2.11.8",
  scalaOrganization := "org.typelevel",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.bintrayIvyRepo("scalameta", "maven")
  ),
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-beta4" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
  scalacOptions ++= Seq(
    "-Ypartial-unification",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-Xplugin-require:macroparadise"
  )
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats" % "0.8.1",
      "io.aecor" %% "liberator" % "0.1.0",
      "io.monix" %% "monix-eval" % "2.1.2",
      "io.monix" %% "monix-cats" % "2.1.2")
  )

