name := "monocle"

organization := "org.kaloz"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8")

scalacOptions += "-Ypartial-unification"

addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.0-MF",
  "com.github.julien-truffaut" %% "monocle-core" % "1.5.0-cats-M1",
  "com.github.julien-truffaut" %% "monocle-macro" % "1.5.0-cats-M1"
)

