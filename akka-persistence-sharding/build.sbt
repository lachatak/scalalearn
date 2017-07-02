name := "kaka-persistence-sharding"

organization := "org.kaloz"

version := "1.0.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

scalaOrganization in ThisBuild := "org.typelevel"

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.9.0",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.3",
  "com.typesafe.akka" %% "akka-persistence-query" % "2.5.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.3",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.54",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.3",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.3",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "0.54" % Test
)

