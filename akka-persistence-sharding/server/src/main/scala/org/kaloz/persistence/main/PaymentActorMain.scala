package org.kaloz.persistence.main

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import org.kaloz.persistence.{ExternalPaymentSystem, PaymentActor}

object PaymentActorMain extends App {

  import org.kaloz.persistence.config.ClusteringConfig._

  val system = ActorSystem(clusterName)

  val listItemPrinterActor = system.actorOf(ExternalPaymentSystem.props())

  ClusterSharding(system).start(
    typeName = PaymentActor.shardName,
    entityProps = PaymentActor.props(listItemPrinterActor),
    settings = ClusterShardingSettings(system),
    messageExtractor = PaymentActor.messageExtractor(10)
  )
}
