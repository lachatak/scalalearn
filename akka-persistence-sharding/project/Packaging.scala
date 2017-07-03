import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import sbt.Keys._
import sbt._

object Packaging {

  lazy val defaultSettings =
    Seq(
      maintainer := "Krisztian Lachata <krisztian.lachata@gmail.com>",
      mainClass in Compile := Some("org.kaloz.persistence.main.ListMaintainerActorMain"),
      dockerExposedPorts in Docker := Seq(1600),
      dockerEntrypoint in Docker := Seq("sh", "-c", "CLUSTER_IP=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1 }'` bin/clustering $*"),
      dockerRepository := Some("lachatak"),
      dockerBaseImage := "relateiq/oracle-java8"
    )

}