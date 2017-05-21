name := "typelevel"

organization := "org.kaloz"

version := "1.0.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

scalaOrganization in ThisBuild := "org.typelevel"

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.9.0",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.atnos" %% "eff" % "4.3.5",
  "org.atnos" %% "eff-monix" % "4.3.5",
  "com.github.mpilquist" %% "simulacrum" % "0.10.0"
)

