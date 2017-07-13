import sbt.Keys._
import sbt._

object Version {

  val cats            = "0.9.0"
  val akka            = "2.5.3"
  val akkaPCassandra  = "0.54"
  val akkaHttp        = "10.0.9"
  val akkaStreamsKafka= "0.16"
  val scalaLogging    = "3.6.0"
  val logBack         = "1.2.3"
  val mockito         = "1.10.19"
  val scalaTest       = "3.0.3"
}

object Library {
  val cats                   = "org.typelevel"               %% "cats"                                  % Version.cats
  val akkaActor              = "com.typesafe.akka"           %% "akka-actor"                            % Version.akka
  val akkaPersistence        = "com.typesafe.akka"           %% "akka-persistence"                      % Version.akka
  val akkaPQuery             = "com.typesafe.akka"           %% "akka-persistence-query"                % Version.akka
  val akkaCluster            = "com.typesafe.akka"           %% "akka-cluster"                          % Version.akka
  val akkaClusterTools       = "com.typesafe.akka"           %% "akka-cluster-tools"                    % Version.akka
  val akkaSharding           = "com.typesafe.akka"           %% "akka-cluster-sharding"                 % Version.akka
  val akkaSlf4j              = "com.typesafe.akka"           %% "akka-slf4j"                            % Version.akka
  val akkapCassandra         = "com.typesafe.akka"           %% "akka-persistence-cassandra"            % Version.akkaPCassandra
  val akkaHttp               = "com.typesafe.akka"           %% "akka-http"                             % Version.akkaHttp
  val akkaStreamsKafka       = "com.typesafe.akka"           %% "akka-stream-kafka"                     % Version.akkaStreamsKafka
  val scalaLogging           = "com.typesafe.scala-logging"  %% "scala-logging"                         % Version.scalaLogging
  val logBackClassic         = "ch.qos.logback"              %  "logback-classic"                       % Version.logBack
  val logBackCore            = "ch.qos.logback"              %  "logback-core"                          % Version.logBack

  val akkaTestkit            = "com.typesafe.akka"           %% "akka-testkit"                          % Version.akka
  val akkaMultiNodeTest      = "com.typesafe.akka"           %% "akka-multi-node-testkit"               % Version.akka
  val akkaPCassandraLauncher = "com.typesafe.akka"           %% "akka-persistence-cassandra-launcher"   % Version.akkaPCassandra
  val scalaTest              = "org.scalatest"               %% "scalatest"                             % Version.scalaTest
  val mockito                = "org.mockito"                 %  "mockito-core"                          % Version.mockito
}

object Dependencies {

  import Library._

  val server = deps(
    akkaActor,
    akkaPersistence,
    akkapCassandra,
    akkaPQuery,
    akkaCluster,
    akkaClusterTools,
    akkaHttp,
    akkaStreamsKafka,
    akkaSharding,
    akkaSlf4j,
    scalaLogging,
    logBackClassic,
    logBackCore,
    akkaTestkit       % "test",
    mockito       	  % "test",
    scalaTest     	  % "test"
  )

  private def deps(modules: ModuleID*): Seq[Setting[_]] = Seq(libraryDependencies ++= modules)
}