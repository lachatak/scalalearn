package org.kaloz.persistence

import java.util.UUID

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import akka.cluster.client.ClusterClient
import akka.cluster.sharding.ShardRegion.{MessageExtractor, Passivate}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.kaloz.persistence.BraintreePaymentActor.{AssignPaymentTokenCommand, ExecuteTransactionCommand, Executed, Executing, ExecutionFinishedEvent, ExecutionFinishedState, ExecutionStartedEvent, ExecutionStartedState, GetPaymentTokenCommand, PAID, PaymentState, PaymentTimedOutEvent, PaymentToken, PaymentTokenAssignedEvent, PaymentTokenRequestedEvent, ProcessExecutionResultCommand, StopPaymentActor, TimedOut, TransactionExecuted, Uninitialised, UninitialisedPaymentState, WaitingForExecution, WaitingForExecutionState, WaitingForToken, WaitingForTokenState}
import org.kaloz.persistence.BrainttreeClientActor.{ExecuteTransaction, GetBraintreeToken}

import scala.concurrent.duration._
import scala.reflect.{ClassTag, _}
import scala.util.{Failure, Success}

class BraintreePaymentActor(clusterClientTarget: String, clusterClient: Option[ActorRef], brainttreeClientActor: ActorRef, eventPublisherActor: ActorRef)
  extends PersistentFSM[BraintreePaymentActor.PaymentFSMState, BraintreePaymentActor.PaymentState, BraintreePaymentActor.Event] with ActorLogging {

  context.setReceiveTimeout(2 minute)

  override def persistenceId = "braintree-payment-" + self.path.name

  log.info(s"STARTED ->> ${self.path}")

  var originalRequester: Option[ActorRef] = None

  override def applyEvent(evt: BraintreePaymentActor.Event, currentState: PaymentState): PaymentState = {
    (evt, currentState) match {
      case (PaymentTokenRequestedEvent(orderId), UninitialisedPaymentState) =>
        log.info(s"Payment $orderId is in WAITING for TOKEN state")
        WaitingForTokenState(orderId)

      case (PaymentTokenAssignedEvent(orderId, token), WaitingForTokenState(_)) =>
        log.info(s"Payment $orderId is in WAITING for EXECUTION state")
        WaitingForExecutionState(orderId, token)

      case (ExecutionStartedEvent(orderId, orderItems), WaitingForExecutionState(_, token)) =>
        log.info(s"Payment $orderId EXECUTION is STARTED")
        ExecutionStartedState(orderId, token, orderItems)

      case (ExecutionFinishedEvent(orderId, referenceId), ExecutionStartedState(_, token, orderItems)) =>
        log.info(s"Payment $orderId EXECUTION is FINISHED")
        ExecutionFinishedState(orderId, token, referenceId, orderItems.map(_.copy(status = PAID)))

      case (e, s) =>
        currentState
    }
  }

  override def domainEventClassTag: ClassTag[BraintreePaymentActor.Event] = classTag[BraintreePaymentActor.Event]

  startWith(Uninitialised, UninitialisedPaymentState)

  when(Uninitialised) {
    case Event(g@GetPaymentTokenCommand(orderId), s) =>
      log.info(s"Arrived $g in $s")
      goto(WaitingForToken) applying PaymentTokenRequestedEvent(orderId) forMax (5 second) andThen {
        case _: WaitingForTokenState =>
          brainttreeClientActor ! GetBraintreeToken(orderId)
          originalRequester = Some(sender())
          log.info(s"$originalRequester for token $orderId")
      }
    case Event(g: ExecuteTransactionCommand, s) =>
      log.warning("Transaction not initialised {} in state {}/{}", g, stateName, s)
      context.parent ! Passivate(stopMessage = Stop)
      sender() ! Left("Transaction Not Initialised!")
      stay
  }

  when(WaitingForToken) {
    case Event(g@AssignPaymentTokenCommand(orderId, token), s) =>
      log.info(s"Arrived $g in $s")
      goto(WaitingForExecution) applying PaymentTokenAssignedEvent(orderId, token) forMax (60 second) andThen {
        case _: WaitingForExecutionState =>
          originalRequester.foreach(_ ! Right(PaymentToken(token.toString)))
          log.info(s"token send for $originalRequester for token $orderId")
      }
  }

  when(WaitingForExecution) {
    case Event(g@ExecuteTransactionCommand(orderId, orderItems), s) =>
      log.info(s"Arrived $g in $s")
      goto(Executing) applying ExecutionStartedEvent(orderId, orderItems) forMax (5 second) andThen {
        case _: ExecutionStartedState =>
          clusterClient.foreach(_ ! ClusterClient.Send(s"/system/sharding/${clusterClientTarget}Payment", StopPaymentActor(orderId), localAffinity = false))
          brainttreeClientActor ! ExecuteTransaction(orderId, orderItems.map(_.amount).sum)
          originalRequester = Some(sender())
          log.info(s"$originalRequester for execute $orderId")
      }
    case Event(g: GetPaymentTokenCommand, s) =>
      log.warning("Transaction Already Started {} in state {}/{}", g, stateName, s)
      context.parent ! Passivate(stopMessage = Stop)
      sender() ! Left("Transaction Already Started!")
      stay
  }

  when(Executing) {
    case Event(g@ProcessExecutionResultCommand(orderId, referenceId), s) =>
      log.info(s"Arrived $g in $s")
      context.parent ! Passivate(stopMessage = Stop)
      val event = ExecutionFinishedEvent(orderId, referenceId)
      goto(Executed) applying event andThen {
        case _: ExecutionFinishedState =>
          originalRequester.foreach(_ ! Right(TransactionExecuted(referenceId.toString)))
          log.info(s"result send for $originalRequester for token $orderId")
          eventPublisherActor ! event
        case x => log.warning(s"->>> unhandled $x - $event")
      }
  }

  when(Executed) {
    case Event(e@(_: GetPaymentTokenCommand | _: ExecuteTransactionCommand), s) =>
      log.warning("Already Executed {} in state {}/{}", e, stateName, s)
      context.parent ! Passivate(stopMessage = Stop)
      sender() ! Left("Already executed!")
      stay
  }

  when(TimedOut) {
    case Event(e@(_: GetPaymentTokenCommand | _: ExecuteTransactionCommand), s) =>
      log.warning("Already TimedOut {} in state {}/{}", e, stateName, s)
      context.parent ! Passivate(stopMessage = Stop)
      sender() ! Left("Already TimedOut!")
      stay
  }

  whenUnhandled {
    case Event(ReceiveTimeout | PersistentFSM.StateTimeout, _) =>
      context.parent ! Passivate(stopMessage = Stop)
      log.warning(s"Timeout --> $self")
      goto(TimedOut) applying PaymentTimedOutEvent()
    case Event(Stop, _) =>
      log.warning(s"Stopped --> $self")
      context.stop(self)
      stay
    case Event(StopPaymentActor(orderId), _) =>
      log.warning(s"Stop processing $orderId")
      context.parent ! Passivate(stopMessage = Stop)
      stay
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      context.parent ! Passivate(stopMessage = Stop)
      stay
  }

}

object BraintreePaymentActor {

  sealed trait Command {
    val orderId: String
  }

  sealed trait Event

  case class GetPaymentTokenCommand(orderId: String) extends Command

  case class PaymentToken(token: String)

  case class AssignPaymentTokenCommand(orderId: String, token: UUID) extends Command

  case class ExecuteTransactionCommand(orderId: String, orderItems: Set[OrderItem]) extends Command

  case class TransactionExecuted(referenceId: String)

  case class ProcessExecutionResultCommand(orderId: String, referenceId: UUID) extends Command

  case class StopPaymentActor(orderId: String) extends Command

  sealed trait OrderItemStatus

  case object UNPAID extends OrderItemStatus

  case object PAID extends OrderItemStatus

  case object ACTIONED extends OrderItemStatus

  case object REFUNDED extends OrderItemStatus

  case object CANCELLED extends OrderItemStatus

  case class OrderItem(orderItemId: String, amount: Long, status: OrderItemStatus = UNPAID)

  sealed trait PaymentFSMState extends FSMState

  case object Uninitialised extends PaymentFSMState {
    override def identifier: String = "Uninitialised"
  }

  case object WaitingForToken extends PaymentFSMState {
    override def identifier: String = "WaitingForToken"
  }

  case object WaitingForExecution extends PaymentFSMState {
    override def identifier: String = "WaitingForExecution"
  }

  case object Executing extends PaymentFSMState {
    override def identifier: String = "Executing"
  }

  case object Executed extends PaymentFSMState {
    override def identifier: String = "Executed"
  }

  case object TimedOut extends PaymentFSMState {
    override def identifier: String = "TimedOut"
  }

  case class PaymentTokenRequestedEvent(orderId: String) extends Event

  case class PaymentTokenAssignedEvent(orderId: String, token: UUID) extends Event

  case class ExecutionStartedEvent(orderId: String, orderItems: Set[OrderItem]) extends Event

  case class PaymentTimedOutEvent() extends Event

  case class ExecutionFinishedEvent(orderId: String, referenceId: UUID) extends Event

  sealed trait PaymentState

  case object UninitialisedPaymentState extends PaymentState

  case class WaitingForTokenState(orderId: String) extends PaymentState

  case class WaitingForExecutionState(orderId: String, token: UUID) extends PaymentState

  case class ExecutionStartedState(orderId: String, token: UUID, orderItems: Set[OrderItem]) extends PaymentState

  case class ExecutionFinishedState(orderId: String, token: UUID, reference: UUID, orderItems: Set[OrderItem]) extends PaymentState

  // Sharding Name
  val shardName: String = "Payment"

  // messageExtractor
  def messageExtractor(numberOfShards: Int): MessageExtractor = PaymentMessageExtractor(numberOfShards)

  private case class PaymentMessageExtractor(numberOfShards: Int) extends MessageExtractor {

    // id extractor
    override def entityId(message: Any): String = message match {
      case c: Command => c.orderId.toString
    }

    // shard resolver
    override def shardId(message: Any): String = message match {
      case c: Command => (c.orderId.toLong % numberOfShards).toString
      case ShardRegion.StartEntity(id) => (id.toLong % numberOfShards).toString
    }

    // get message
    override def entityMessage(message: Any): Any = message match {
      case command: Command => command
    }
  }

  def props(clusterClientTarget: String, clusterClient: Option[ActorRef], brainttreeClientActor: ActorRef, eventPublisherActor: ActorRef) = Props(new BraintreePaymentActor(clusterClientTarget, clusterClient, brainttreeClientActor, eventPublisherActor))
}

class BrainttreeClientActor extends Actor with ActorLogging {

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  import org.kaloz.persistence.config.ClusteringConfig.clusterName

  override def receive = {
    case GetBraintreeToken(orderId) =>
      log.info(s"Client -> Get braintree TOKEN for $orderId")
      //      Http().singleRequest(HttpRequest(uri = "http://akka.io"))
      //        .map(_ => AssignPaymentTokenCommand(orderId, UUID.randomUUID())) pipeTo sender()
      val braintreeRegion: ActorRef = ClusterSharding(context.system).shardRegion(clusterName + BraintreePaymentActor.shardName)

      log.info(s"$braintreeRegion is available in BrainttreeClientActor")

      braintreeRegion ! AssignPaymentTokenCommand(orderId, UUID.randomUUID())

    case ExecuteTransaction(orderId, amount: Long) =>
      log.info(s"Client -> Execute transaction for $orderId with $amount")
      //      Http().singleRequest(HttpRequest(uri = "http://akka.io"))
      //        .map(_ => ProcessExecutionResultCommand(orderId, UUID.randomUUID())) pipeTo sender()
      val braintreeRegion: ActorRef = ClusterSharding(context.system).shardRegion(clusterName + BraintreePaymentActor.shardName)

      log.info(s"$braintreeRegion is available in BrainttreeClientActor")

      braintreeRegion ! ProcessExecutionResultCommand(orderId, UUID.randomUUID())
  }
}

object BrainttreeClientActor {

  case class GetBraintreeToken(orderId: String)

  case class ExecuteTransaction(orderId: String, amount: Long)

  def props() = Props[BrainttreeClientActor]
}

class EventPublisherActor(kafkaIp: String) extends Actor with ActorLogging {

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  override def receive = {
    case e@ExecutionFinishedEvent(orderId, paymentId) =>
      log.info(s"Sending $e")

      val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
        .withBootstrapServers(s"$kafkaIp:9092")

      val kafkaSink = Producer.plainSink(producerSettings)

      val (control, future) = Source.single(e)
        .map { event =>
          new ProducerRecord[Array[Byte], String]("test", event.orderId.getBytes, s"""{"order_id":$orderId, "payment_id":"$paymentId"}""")
        }
        .toMat(kafkaSink)(Keep.both)
        .run()

      future.onComplete {
        case Failure(ex) =>
          log.error(s"Stream failed due to error, restarting: $ex")
          throw ex
        case Success(s) =>
          log.info(s"Sucess --->>>> $s")
      }
  }
}

object EventPublisherActor {
  def props(kafkaIp: String) = Props(new EventPublisherActor(kafkaIp))
}
