package org.kaloz.persistence

import java.util.UUID

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, FSM, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{MessageExtractor, Passivate}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.pattern.pipe
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.kaloz.persistence.BraintreePaymentActor.{AssignPaymentTokenCommand, ExecuteTransactionCommand, Executed, Executing, ExecutionFinishedEvent, ExecutionFinishedState, ExecutionStartedEvent, ExecutionStartedState, GetPaymentTokenCommand, PAID, PaymentState, PaymentTimedOutEvent, PaymentToken, PaymentTokenAssignedEvent, PaymentTokenRequestedEvent, ProcessExecutionResultCommand, TimedOut, TransactionExecuted, Uninitialised, UninitialisedPaymentState, WaitingForExecution, WaitingForExecutionState, WaitingForToken, WaitingForTokenState}
import org.kaloz.persistence.BrainttreeClientActor.{ExecuteTransaction, GetBraintreeToken}
import org.kaloz.persistence.config.ClusteringConfig.clusterName

import scala.concurrent.duration._
import scala.reflect.{ClassTag, _}
import scala.util.{Failure, Success}

class BraintreePaymentActor(brainttreeClientActor: ActorRef, eventPublisherActor: ActorRef)
  extends PersistentFSM[BraintreePaymentActor.PaymentFSMState, BraintreePaymentActor.PaymentState, BraintreePaymentActor.Event] with ActorLogging {

  context.setReceiveTimeout(2 minute)

  override def persistenceId = "braintree-payment-" + self.path.name


  override def applyEvent(evt: BraintreePaymentActor.Event, currentState: PaymentState): PaymentState = {
    (evt, currentState) match {
      case (PaymentTokenRequestedEvent(orderId, requester), _) =>
        log.info(s"Payment $orderId is in WAITING for TOKEN state")
        WaitingForTokenState(orderId, requester)

      case (PaymentTokenAssignedEvent(orderId, token), _) =>
        log.info(s"Payment $orderId is in WAITING for EXECUTION state")
        WaitingForExecutionState(orderId, token)

      case (ExecutionStartedEvent(orderId, orderItems, requester), WaitingForExecutionState(_, token)) =>
        log.info(s"Payment $orderId EXECUTION is STARTED")
        ExecutionStartedState(orderId, token, orderItems, requester)

      case (ExecutionFinishedEvent(orderId, referenceId), ExecutionStartedState(_, token, orderItems, _)) =>
        log.info(s"Payment $orderId EXECUTION is FINISHED")
        ExecutionFinishedState(orderId, token, referenceId, orderItems.map(_.copy(status = PAID)))

      case (_, _) =>
        currentState
    }
  }

  override def domainEventClassTag: ClassTag[BraintreePaymentActor.Event] = classTag[BraintreePaymentActor.Event]

  startWith(Uninitialised, UninitialisedPaymentState)

  when(Uninitialised) {
    case Event(g@GetPaymentTokenCommand(orderId), _) =>
      brainttreeClientActor ! GetBraintreeToken(orderId)
      goto(WaitingForToken) applying PaymentTokenRequestedEvent(orderId, sender()) forMax (5 second) //TIMEOUT for the external call
  }

  when(WaitingForToken) {
    case Event(AssignPaymentTokenCommand(orderId, token), WaitingForTokenState(_, requester)) =>
      requester ! PaymentToken(token)
      goto(WaitingForExecution) applying PaymentTokenAssignedEvent(orderId, token) forMax (60 second) //TIMEOUT for EXECUTION CALL
  }

  when(WaitingForExecution) {
    case Event(ExecuteTransactionCommand(orderId, orderItems), _) =>
      brainttreeClientActor ! ExecuteTransaction(orderId, orderItems.map(_.amount).sum)
      goto(Executing) applying ExecutionStartedEvent(orderId, orderItems, sender()) forMax (5 second) //TIMEOUT for the external call
  }

  when(Executing) {
    case Event(ProcessExecutionResultCommand(orderId, referenceId), ExecutionStartedState(_, _, _, requester)) =>
      requester ! TransactionExecuted(referenceId)
      context.parent ! Passivate(stopMessage = Stop)
      val event = ExecutionFinishedEvent(orderId, referenceId)
      goto(Executed) applying event andThen {
        case _: ExecutionFinishedState =>
          eventPublisherActor ! event
      }
  }

  when(Executed)(FSM.NullFunction)

  when(TimedOut)(FSM.NullFunction)

  whenUnhandled {
    case Event(ReceiveTimeout | PersistentFSM.StateTimeout, _) =>
      context.parent ! Passivate(stopMessage = Stop)
      log.warning(s"Timeout --> $self")
      goto(TimedOut) applying PaymentTimedOutEvent()
    case Event(Stop, _) =>
      log.warning(s"Stopped --> $self")
      context.stop(self)
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

  case class PaymentToken(token: UUID)

  case class AssignPaymentTokenCommand(orderId: String, token: UUID) extends Command

  case class ExecuteTransactionCommand(orderId: String, orderItems: Set[OrderItem]) extends Command

  case class TransactionExecuted(referenceId: UUID)

  case class ProcessExecutionResultCommand(orderId: String, referenceId: UUID) extends Command

  sealed trait OrderItemStatus

  case object UNPAID extends OrderItemStatus

  case object PAID extends OrderItemStatus

  case object ACTIONED extends OrderItemStatus

  case object REFUNDED extends OrderItemStatus

  case object CANCELLED extends OrderItemStatus

  case class OrderItem(orderItemId: UUID, amount: Long, status: OrderItemStatus = UNPAID)

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

  case class PaymentTokenRequestedEvent(orderId: String, requester: ActorRef) extends Event

  case class PaymentTokenAssignedEvent(orderId: String, toke: UUID) extends Event

  case class ExecutionStartedEvent(orderId: String, orderItems: Set[OrderItem], requester: ActorRef) extends Event

  case class PaymentTimedOutEvent() extends Event

  case class ExecutionFinishedEvent(orderId: String, referenceId: UUID) extends Event

  sealed trait PaymentState

  case object UninitialisedPaymentState extends PaymentState

  case class WaitingForTokenState(orderId: String, requester: ActorRef) extends PaymentState

  case class WaitingForExecutionState(orderId: String, token: UUID) extends PaymentState

  case class ExecutionStartedState(orderId: String, token: UUID, orderItems: Set[OrderItem], requester: ActorRef) extends PaymentState

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

  def props(brainttreeClientActor: ActorRef, eventPublisherActor: ActorRef) = Props(new BraintreePaymentActor(brainttreeClientActor, eventPublisherActor))
}

class BrainttreeClientActor extends Actor with ActorLogging {

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  override def receive = {
    case GetBraintreeToken(orderId) =>
      log.info(s"Client -> Get braintree TOKEN for $orderId")
      Http().singleRequest(HttpRequest(uri = "http://akka.io"))
        .map(_ => AssignPaymentTokenCommand(orderId, UUID.randomUUID())) pipeTo sender()

    case ExecuteTransaction(orderId, amount: Long) =>
      log.info(s"Client -> Execute transaction for $orderId with $amount")
      Http().singleRequest(HttpRequest(uri = "http://akka.io"))
        .map(_ => ProcessExecutionResultCommand(orderId, UUID.randomUUID())) pipeTo sender()
  }
}

object BrainttreeClientActor {

  case class GetBraintreeToken(orderId: String)

  case class ExecuteTransaction(orderId: String, amount: Long)

  def props() = Props[BrainttreeClientActor]
}

class EventPublisherActor(kafkaIp:String) extends Actor with ActorLogging {

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
  def props(kafkaIp:String) = Props(new EventPublisherActor(kafkaIp))
}

object Test extends App {

  implicit val system = ActorSystem(clusterName)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val log: LoggingAdapter = Logging.getLogger(system, this)

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("127.0.0.1:9092")

  val kafkaSink = Producer.plainSink(producerSettings)

  val (control, future) = Source.single(ExecutionFinishedEvent("17", UUID.randomUUID()))
    .map { event =>
      new ProducerRecord[Array[Byte], String]("test", event.orderId.getBytes, s"""{"order_id":${event.orderId}, "payment_id":"${event.referenceId}"}""")
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