name := "zip"

organization := "org.kaloz"

version := "1.0.0"

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8")

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.3" % "test",
  "junit" % "junit" % "4.12"
)
