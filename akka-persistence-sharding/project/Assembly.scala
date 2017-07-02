import sbt.Keys._
import sbtassembly.AssemblyKeys._
import sbtassembly.{MergeStrategy, PathList}

object Assembly {

  lazy val defaultSettings =
    Seq(
      mainClass in assembly := Some("org.kaloz.persistence.ListMaintainerActorMain"),
      assemblyJarName in assembly := s"${name.value}-${version.value}-assembly.jar",
      assemblyMergeStrategy in assembly := {
        case PathList("META-INF", xs @ _*) => MergeStrategy.discard
        case "application.conf" => MergeStrategy.concat
        case "reference.conf" => MergeStrategy.concat
        case x => MergeStrategy.first
      }

    )

}