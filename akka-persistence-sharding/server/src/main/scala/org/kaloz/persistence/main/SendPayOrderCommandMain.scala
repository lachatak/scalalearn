package org.kaloz.persistence.main

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import org.kaloz.persistence.BraintreePaymentActor._
import org.kaloz.persistence.{BrainttreeClientActor, BraintreePaymentActor}

object SendPayOrderCommandMain extends App {

  import org.kaloz.persistence.config.ClusteringConfig._

  val system = ActorSystem(clusterName)

  val listItemPrinterActor = system.actorOf(BrainttreeClientActor.props())

  ClusterSharding(system).start(
    typeName = BraintreePaymentActor.shardName,
    entityProps = BraintreePaymentActor.props(listItemPrinterActor),
    settings = ClusterShardingSettings(system),
    messageExtractor = BraintreePaymentActor.messageExtractor(10)
  )

  val cluster = Cluster(system)
  val braintreeRegion: ActorRef = ClusterSharding(system).shardRegion(BraintreePaymentActor.shardName)

  braintreeRegion ! GetPaymentTokenCommand("1")
  braintreeRegion ! ExecuteTransactionCommand("1", Set(OrderItem(UUID.randomUUID(), 10), OrderItem(UUID.randomUUID(), 20)))

  Thread.sleep(10000)

  braintreeRegion ! GetPaymentTokenCommand("2")

  Thread.sleep(1000)

  system.actorOf(Props(new Actor with ActorLogging {
    context.watch(braintreeRegion)

    def receive = {
      case Terminated(listMaintainerRegion) =>
        log.info(s"Terminated $listMaintainerRegion")
        cluster.registerOnMemberRemoved(context.system.terminate())
        cluster.leave(cluster.selfAddress)
    }
  }))

  braintreeRegion ! ShardRegion.GracefulShutdown
}
