package org.kaloz.persistence.main

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import org.kaloz.persistence.ListMaintainerActor.AddItemCommand
import org.kaloz.persistence.{ListItemPrinterActor, ListMaintainerActor}

object SendListItemsMain extends App {

  import org.kaloz.persistence.config.ClusteringConfig._

  val system = ActorSystem(clusterName)

  val listItemPrinterActor = system.actorOf(ListItemPrinterActor.props())

  ClusterSharding(system).start(
    typeName = ListMaintainerActor.shardName,
    entityProps = ListMaintainerActor.props(listItemPrinterActor),
    settings = ClusterShardingSettings(system),
    messageExtractor = ListMaintainerActor.messageExtractor(10)
  )

  val cluster = Cluster(system)
  val listMaintainerRegion: ActorRef = ClusterSharding(system).shardRegion(ListMaintainerActor.shardName)

  listMaintainerRegion ! AddItemCommand(UUID.randomUUID(), "list1", "foo")
  listMaintainerRegion ! AddItemCommand(UUID.randomUUID(), "list1", "bar")

  listMaintainerRegion ! AddItemCommand(UUID.randomUUID(), "list2", "foo")

  Thread.sleep(10000)

  listMaintainerRegion ! AddItemCommand(UUID.randomUUID(), "list1", "foo2")

  Thread.sleep(1000)


  system.actorOf(Props(new Actor with ActorLogging {
    context.watch(listMaintainerRegion)

    def receive = {
      case Terminated(listMaintainerRegion) =>
        log.info(s"Terminated $listMaintainerRegion")
        cluster.registerOnMemberRemoved(context.system.terminate())
        cluster.leave(cluster.selfAddress)
    }
  }))

  listMaintainerRegion ! ShardRegion.GracefulShutdown
}
