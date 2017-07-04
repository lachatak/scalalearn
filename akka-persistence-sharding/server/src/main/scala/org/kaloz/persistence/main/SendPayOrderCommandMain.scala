package org.kaloz.persistence.main

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import org.kaloz.persistence.PaymentActor.PayOrderCommand
import org.kaloz.persistence.{ExternalPaymentSystem, PaymentActor}

object SendPayOrderCommandMain extends App {

  import org.kaloz.persistence.config.ClusteringConfig._

  val system = ActorSystem(clusterName)

  val listItemPrinterActor = system.actorOf(ExternalPaymentSystem.props())

  ClusterSharding(system).start(
    typeName = PaymentActor.shardName,
    entityProps = PaymentActor.props(listItemPrinterActor),
    settings = ClusterShardingSettings(system),
    messageExtractor = PaymentActor.messageExtractor(10)
  )

  val cluster = Cluster(system)
  val listMaintainerRegion: ActorRef = ClusterSharding(system).shardRegion(PaymentActor.shardName)

  listMaintainerRegion ! PayOrderCommand(UUID.randomUUID(), 20)
  listMaintainerRegion ! PayOrderCommand(UUID.randomUUID(), 20)

  listMaintainerRegion ! PayOrderCommand(UUID.randomUUID(), 10)

  Thread.sleep(10000)

  listMaintainerRegion ! PayOrderCommand(UUID.randomUUID(), 15)

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
