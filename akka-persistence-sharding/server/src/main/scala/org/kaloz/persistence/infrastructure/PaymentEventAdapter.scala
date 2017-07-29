package org.kaloz.persistence.infrastructure

import java.util.UUID

import akka.persistence.journal.{EventAdapter, EventSeq}
import org.kaloz.persistence.BraintreePaymentActor.{ExecutionFinishedEvent, ExecutionStartedEvent, OrderItem, PaymentTimedOutEvent, PaymentTokenAssignedEvent, PaymentTokenRequestedEvent}
import org.kaloz.persistence.infrastructure.PaymentEventAdapter.{ExecutionFinishedPersistenceV1, ExecutionStartedPersistenceV1, PaymentTimedOutPersistenceV1, PaymentTokenAssignedPersistenceV1, PaymentTokenRequestedPersistenceV1}

/**
  * This class upgrades old events from the event journal
  *
  * actor -> message -> event adapter -> serializer -> journal
  * actor <- message <- event adapter <- serializer <- journal
  */
class PaymentEventAdapter extends EventAdapter {

  override def manifest(event: Any): String = event.getClass.getName

  // Pass through to serializer since I'm always persisting the latest events
  override def toJournal(event: Any): Any = event match {
    case PaymentTokenRequestedEvent(orderId) => PaymentTokenRequestedPersistenceV1(orderId)
    case PaymentTokenAssignedEvent(orderId, token) => PaymentTokenAssignedPersistenceV1(orderId, token.toString)
    case ExecutionStartedEvent(orderId, orderItems) => ExecutionStartedPersistenceV1(orderId, orderItems)
    case PaymentTimedOutEvent() => PaymentTimedOutPersistenceV1()
    case ExecutionFinishedEvent(orderId, referenceId) => ExecutionFinishedPersistenceV1(orderId, referenceId.toString)
  }

  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case PaymentTokenRequestedPersistenceV1(orderId) => EventSeq(PaymentTokenRequestedEvent(orderId))
    case PaymentTokenAssignedPersistenceV1(orderId, token) => EventSeq(PaymentTokenAssignedEvent(orderId, UUID.fromString(token)))
    case ExecutionStartedPersistenceV1(orderId, orderItems) => EventSeq(ExecutionStartedEvent(orderId, orderItems))
    case PaymentTimedOutPersistenceV1() => EventSeq(PaymentTimedOutEvent())
    case ExecutionFinishedPersistenceV1(orderId, referenceId) => EventSeq(ExecutionFinishedEvent(orderId, UUID.fromString(referenceId)))
  }
}

object PaymentEventAdapter {

  sealed trait Persistence extends Serializable

  case class PaymentTokenRequestedPersistenceV1(orderId: String) extends Persistence

  case class PaymentTokenAssignedPersistenceV1(orderId: String, token: String) extends Persistence

  case class ExecutionStartedPersistenceV1(orderId: String, orderItems: Set[OrderItem]) extends Persistence

  case class PaymentTimedOutPersistenceV1() extends Persistence

  case class ExecutionFinishedPersistenceV1(orderId: String, referenceId: String) extends Persistence

}
