import sbt.Keys._
import sbt._

object Resolvers {

  lazy val defaultSettings = Seq(resolvers ++=
    Seq(
      Resolver.defaultLocal,
      Resolver.mavenLocal,
      Resolver.typesafeRepo("snapshots"),
      Resolver.typesafeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases")
    )
  )
}