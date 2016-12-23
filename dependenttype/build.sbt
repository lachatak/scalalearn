name := "dependenttype-test"

organization := "org.kaloz"

version := "1.0.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8")

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.4.1",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.scalaz" %% "scalaz-core" % "7.2.2"
)

