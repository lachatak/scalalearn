package org.kaloz.persistence.main

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import org.kaloz.persistence.{ListItemPrinterActor, ListMaintainerActor}

object ListMaintainerActorMain extends App {

  import org.kaloz.persistence.config.ClusteringConfig._

  val system = ActorSystem(clusterName)

  val listItemPrinterActor = system.actorOf(ListItemPrinterActor.props())

  ClusterSharding(system).start(
    typeName = ListMaintainerActor.shardName,
    entityProps = ListMaintainerActor.props(listItemPrinterActor),
    settings = ClusterShardingSettings(system),
    messageExtractor = ListMaintainerActor.messageExtractor(10)
  )
}
