package org.kaloz.persistence.main

import java.util.UUID

import akka.actor.{ActorPath, ActorRef, ActorSystem}
import akka.cluster.client.{ClusterClient, ClusterClientReceptionist, ClusterClientSettings}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.kaloz.persistence.BraintreePaymentActor.{ExecuteTransactionCommand, GetPaymentTokenCommand, OrderItem, PaymentToken, TransactionExecuted}
import org.kaloz.persistence.{BraintreePaymentActor, BrainttreeClientActor, EventPublisherActor}

import scala.concurrent.duration._

object PaymentActorMain extends App {

  import org.kaloz.persistence.config.ClusteringConfig._

  implicit val system = ActorSystem(clusterName)
  val log: LoggingAdapter = Logging.getLogger(system, this)

  val clusterClient: Option[ActorRef] = if (withReceptionist) {
    val initialContacts = Set(
      ActorPath.fromString(s"akka.tcp://$receptionistName@$receptionistIp:$receptionistPort/system/receptionist"))
    val settings = ClusterClientSettings(system).withInitialContacts(initialContacts)

    val clusterClient = system.actorOf(ClusterClient.props(settings), "client")
    Some(clusterClient)
  } else {
    None
  }

  val brainttreeClientActor = system.actorOf(BrainttreeClientActor.props())
  val eventPublisherActor = system.actorOf(EventPublisherActor.props(kafkaIp))

  ClusterSharding(system).start(
    typeName = clusterName + BraintreePaymentActor.shardName,
    entityProps = BraintreePaymentActor.props(receptionistName, clusterClient, brainttreeClientActor, eventPublisherActor),
    settings = ClusterShardingSettings(system),
    messageExtractor = BraintreePaymentActor.messageExtractor(10)
  )

  log.info("Starting HTTP!!!!!")

  import akka.http.scaladsl.Http
  import akka.http.scaladsl.server.Directives._
  import akka.pattern.ask

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val braintreeRegion: ActorRef = ClusterSharding(system).shardRegion(clusterName + BraintreePaymentActor.shardName)

  if (withReceptionist)
    ClusterClientReceptionist(system).registerService(braintreeRegion)

  log.info(s"REGION -->> $braintreeRegion")

  implicit val timeout = Timeout(5 seconds)

  val route =
    path("token" / Segment) { orderId =>
      get {
        onSuccess((braintreeRegion ? GetPaymentTokenCommand(orderId)).mapTo[PaymentToken]) { token =>
          complete(token.token.toString)
        }
      }
    } ~ path("execute" / Segment) { orderId =>
      get {
        onSuccess((braintreeRegion ? ExecuteTransactionCommand(orderId, Set(OrderItem(UUID.randomUUID().toString, 10), OrderItem(UUID.randomUUID().toString, 20)))).mapTo[TransactionExecuted]) { ref =>
          complete(ref.referenceId.toString)
        }
      }
    }

  val host = "0.0.0.0"

  val bindingFuture = Http().bindAndHandle(route, host, httpPort)

  log.info(s"Application has been started and listening on $host:$httpPort")
  //    bindingFuture
  //      .flatMap(_.unbind()) // trigger unbinding from the port
  //      .onComplete(_ => system.terminate()) // and shutdown when done
}
