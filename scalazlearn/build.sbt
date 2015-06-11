name := "scalalearn"

organization := "org.kaloz"

version := "1.0.0"

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8")

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
