package org.kaloz.persistence

import java.util.UUID

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion.{MessageExtractor, Passivate}
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import akka.persistence.{AtLeastOnceDelivery, PersistentActor, RecoveryCompleted, SnapshotOffer}
import org.kaloz.persistence.ExternalPaymentSystem.SendPaymentMessageCommand
import org.kaloz.persistence.PaymentActor.{DeliveryStateSnapshot, OrderPaymentFailedEvent, OrderPaymentInitialisedEvent, OrderPaymentSuccessfulEvent, PayOrderCommand, PaymentState, Uninitialised}

import scala.concurrent.duration._

sealed trait Command

sealed trait Event

class PaymentActor(externalPaymentSystem: ActorRef) extends PersistentActor with AtLeastOnceDelivery with ActorLogging {

  context.setReceiveTimeout(5.seconds)

  override def persistenceId = "Payment-" + self.path.name

  var state: PaymentState = PaymentState()

  def updateState(event: Event): Unit = event match {
    case OrderPaymentInitialisedEvent(paymentId, orderId, amount) =>
      state = state.toPendingPayment(paymentId, orderId, amount)
      deliver(externalPaymentSystem.path)(deliveryId => SendPaymentMessageCommand(deliveryId, paymentId, orderId, amount))
    case OrderPaymentSuccessfulEvent(deliveryId) =>
      confirmDelivery(deliveryId)
      log.info(s"Success")
      state = state.toPaidPayment()
      val snapshot = DeliveryStateSnapshot(state, getDeliverySnapshot)
      saveSnapshot(snapshot)
      log.info(s"Snapshot saved --> $snapshot")
    case OrderPaymentFailedEvent(deliveryId) =>
      confirmDelivery(deliveryId)
      log.info(s"Success")
      state = state.toFailedPayment()
      val snapshot = DeliveryStateSnapshot(state, getDeliverySnapshot)
      saveSnapshot(snapshot)
      log.info(s"Snapshot saved --> $snapshot")
  }

  val receiveRecover: Receive = {
    case evt: Event =>
      log.info(s"Recover with $evt")
      updateState(evt)
    case SnapshotOffer(_, d@DeliveryStateSnapshot(snapshot, alodSnapshot)) =>
      log.info(s"SnapshotOffered with $d")
      setDeliverySnapshot(alodSnapshot)
      state = snapshot
    case RecoveryCompleted => log.info("RecoveryCompleted")
  }

  val receiveCommand: Receive = {
    case p@PayOrderCommand(orderId, amount) =>
      if (state.status == Uninitialised) {
        persist(OrderPaymentInitialisedEvent(p.paymentId, orderId, amount))(updateState)
      }
      else {
        log.info(s"${p.paymentId} has been already processed!")
      }
    case o@(_: OrderPaymentSuccessfulEvent | _: OrderPaymentFailedEvent) => persist(o.asInstanceOf[Event])(updateState)
    case AtLeastOnceDelivery.UnconfirmedWarning(deliveries) => log.info(s"UnconfirmedWarning $deliveries!!")
    //Passivate
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      log.info(s"$persistenceId is stopping...")
      context.stop(self)
  }

}

object PaymentActor {

  case class PayOrderCommand(orderId: UUID, amount: Long) extends Command {
    val paymentId: UUID = UUID.randomUUID()
  }

  case class OrderPaymentInitialisedEvent(paymentId: UUID, orderId: UUID, amount: Long) extends Event

  case class OrderPaymentSuccessfulEvent(deliveryId: Long) extends Event

  case class OrderPaymentFailedEvent(deliveryId: Long) extends Event

  sealed trait PaymentStatus

  case object Uninitialised extends PaymentStatus

  case object Pending extends PaymentStatus

  case object Paid extends PaymentStatus

  case object Failed extends PaymentStatus

  case class PaymentState(paymentId: Option[UUID] = None,
                          orderId: Option[UUID] = None,
                          amount: Option[Long] = None,
                          status: PaymentStatus = Uninitialised) {

    def toPendingPayment(paymentId: UUID, orderId: UUID, amount: Long) = PaymentState(Option(paymentId), Option(orderId), Option(amount), Pending)

    def toPaidPayment() = copy(status = Paid)

    def toFailedPayment() = copy(status = Failed)
  }

  //save state with AtLeastOnceDeliverySnapshot
  case class DeliveryStateSnapshot(payment: PaymentState, alodSnapshot: AtLeastOnceDeliverySnapshot)

  // Sharding Name
  val shardName: String = "Payment"

  // messageExtractor
  def messageExtractor(numberOfShards: Int): MessageExtractor = PaymentMessageExtractor(numberOfShards)

  private case class PaymentMessageExtractor(numberOfShards: Int) extends MessageExtractor {

    // id extractor
    override def entityId(message: Any): String = message match {
      case p@PayOrderCommand(_, _) => p.paymentId.toString
    }

    // shard resolver
    override def shardId(message: Any): String = message match {
      case p@PayOrderCommand(_, _) => (p.paymentId.hashCode % numberOfShards).toString
    }

    // get message
    override def entityMessage(message: Any): Any = message match {
      case msg@PayOrderCommand(_, _) => msg
    }
  }

  def props(target: ActorRef) = Props(new PaymentActor(target))
}

class ExternalPaymentSystem extends Actor with ActorLogging {

  override def receive = {
    case SendPaymentMessageCommand(deliveryId, paymentId, orderId, amount) =>
      //      if (data.startsWith("foo")) {
      log.info(s"CONFIRMING!! *$deliveryId) $paymentId-$orderId with value $amount")
      sender() ! OrderPaymentSuccessfulEvent(deliveryId)
    //      } else {
    //        log.info(s"NOT CONFIRMING !! $deliveryId was delivered with value $data!")
    //      }
  }
}

object ExternalPaymentSystem {

  case class SendPaymentMessageCommand(deliveryId: Long, paymentId: UUID, orderId: UUID, amount: Long) extends Command

  def props() = Props[ExternalPaymentSystem]
}
