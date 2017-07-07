package org.kaloz.persistence.main

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.kaloz.persistence.BraintreePaymentActor.{ExecuteTransactionCommand, GetPaymentTokenCommand, OrderItem, PaymentToken, TransactionExecuted}
import org.kaloz.persistence.{BraintreePaymentActor, BrainttreeClientActor}

import scala.concurrent.duration._

object PaymentActorMain extends App {

  import org.kaloz.persistence.config.ClusteringConfig._

  implicit val system = ActorSystem(clusterName)
  val log: LoggingAdapter = Logging.getLogger(system, this)

  val brainttreeClientActor = system.actorOf(BrainttreeClientActor.props())

  ClusterSharding(system).start(
    typeName = BraintreePaymentActor.shardName,
    entityProps = BraintreePaymentActor.props(brainttreeClientActor),
    settings = ClusterShardingSettings(system),
    messageExtractor = BraintreePaymentActor.messageExtractor(10)
  )

  if (port == 1600) {

    log.info("Starting HTTP!!!!!")
    //TODO for testing
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.server.Directives._
    import akka.pattern.ask

    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val braintreeRegion: ActorRef = ClusterSharding(system).shardRegion(BraintreePaymentActor.shardName)

    implicit val timeout = Timeout(5 seconds)

    val route =
      path("token"/ Segment) { orderId =>
        get {
          onSuccess((braintreeRegion ? GetPaymentTokenCommand(orderId)).mapTo[PaymentToken]) { token =>
            complete(token.token.toString)
          }
        }
      } ~ path("execute"/ Segment) { orderId =>
      get {
        onSuccess((braintreeRegion ? ExecuteTransactionCommand(orderId, Set(OrderItem(UUID.randomUUID(), 10), OrderItem(UUID.randomUUID(), 20)))).mapTo[TransactionExecuted]) { ref =>
          complete(ref.referenceId.toString)
        }
      }
    }

    val host = "0.0.0.0"
    val port = 8080

    val bindingFuture = Http().bindAndHandle(route, host, port)

    log.info(s"Application has been started and listening on $host:$port")
//    bindingFuture
//      .flatMap(_.unbind()) // trigger unbinding from the port
//      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
