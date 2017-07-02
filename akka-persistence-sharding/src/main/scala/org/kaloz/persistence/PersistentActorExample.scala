package org.kaloz.persistence

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

sealed trait Command

case class UpdateListCommand(data: String) extends Command

case class PrintListItemCommand(deliveryId: Long, data: String) extends Command

case class Confirm(deliveryId: Long, data: String) extends Command


sealed trait Event

case class ListUpdatedEvent(data: String) extends Event

case class ListItemPrintedEvent(deliveryId: Long, data: String) extends Event

case class ExampleState(items: List[String] = Nil) {
  def updated(item: String): ExampleState = copy(item :: items)

  def size: Int = items.length

  override def toString: String = items.reverse.toString
}

//save state with AtLeastOnceDeliverySnapshot
case class DeliveryStateSnapshot(exampleState: ExampleState, alodSnapshot: AtLeastOnceDeliverySnapshot)

class ExamplePersistentActor(target: ActorRef) extends PersistentActor with AtLeastOnceDelivery {
  override def persistenceId = "sample-id-1"

  var state = ExampleState()

  def updateState(event: Event): Unit = event match {
    case ListUpdatedEvent(data) =>
      deliver(target.path)(deliveryId => PrintListItemCommand(deliveryId, data))

    case l@ListItemPrintedEvent(deliveryId, data) =>
      confirmDelivery(deliveryId)
      println(s"ListItemPrintedEvent --> $l")
      state = state.updated(data)
      val snapshot = DeliveryStateSnapshot(state, getDeliverySnapshot)
      saveSnapshot(snapshot)
      println(s"Snapshot saved --> $snapshot")
  }

  def numEvents = state.size

  val receiveRecover: Receive = {
    case evt: Event =>
      println(s"Recover with $evt")
      updateState(evt)
    case SnapshotOffer(_, d@DeliveryStateSnapshot(snapshot, alodSnapshot)) =>
      println(s"SnapshotOffered with $d")
      setDeliverySnapshot(alodSnapshot)
      state = snapshot
    case RecoveryCompleted => println("RecoveryCompleted")
  }

  val receiveCommand: Receive = {
    case UpdateListCommand(data) => persist(ListUpdatedEvent(s"${data}-${numEvents}"))(updateState)
    case Confirm(deliveryId, data) => persist(ListItemPrintedEvent(deliveryId, data))(updateState)
    case AtLeastOnceDelivery.UnconfirmedWarning(deliveries) => println(s"UnconfirmedWarning $deliveries!!")
  }

}

class TargetActor extends Actor {

  override def receive = {
    case PrintListItemCommand(deliveryId, data) =>
//      if (data.startsWith("foo")) {
        println(s"CONFIRMING!! $deliveryId was delivered with value $data!")
        sender() ! Confirm(deliveryId, data)
//      } else {
//        println(s"NOT CONFIRMING !! $deliveryId was delivered with value $data!")
//      }
  }

}

object PersistentActorExample extends App {

  val system = ActorSystem("example")

  implicit val mat = ActorMaterializer()(system)

  val targetActor = system.actorOf(Props[TargetActor])
  val persistentActor = system.actorOf(Props(new ExamplePersistentActor(targetActor)), "persistentActor-4-scala")

    val queries = PersistenceQuery(system).readJournalFor[CassandraReadJournal](
      CassandraReadJournal.Identifier
    )

    val evts: Source[EventEnvelope, NotUsed] = queries.eventsByPersistenceId("sample-id-1", 0, Long.MaxValue)


    evts.runForeach { evt => println(s"Event: $evt") }

    Thread.sleep(3000)
//    persistentActor ! UpdateListCommand("foo")
//    persistentActor ! UpdateListCommand("foo")
//    persistentActor ! UpdateListCommand("foo")
//    persistentActor ! UpdateListCommand("baz")

  //  Thread.sleep(10000)
  //  system.terminate()
}
