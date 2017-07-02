package org.kaloz.persistence

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout, Terminated}
import akka.cluster.Cluster
import akka.cluster.sharding.ShardRegion.{MessageExtractor, Passivate}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import akka.persistence.{AtLeastOnceDelivery, PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.typesafe.config.ConfigFactory
import org.kaloz.persistence.ListItemPrinterActor.PrintListItemCommand
import org.kaloz.persistence.ListMaintainerActor.{AddItemCommand, ConfirmPrinted, DeliveryStateSnapshot, ItemAddedEvent, ItemPrintedEvent, ListState}

import scala.concurrent.duration._

sealed trait Command

sealed trait Event

class ListMaintainerActor(listItemPrinterActor: ActorRef) extends PersistentActor with AtLeastOnceDelivery {

  context.setReceiveTimeout(5.seconds)

  override def persistenceId = "List-" + self.path.name

  var state = ListState()

  def updateState(event: Event): Unit = event match {
    case ItemAddedEvent(id, data) =>
      deliver(listItemPrinterActor.path)(deliveryId => PrintListItemCommand(deliveryId, id, data))
    case l@ItemPrintedEvent(deliveryId, id, data) =>
      confirmDelivery(deliveryId)
      println(s"ListItemPrintedEvent --> $l")
      state = state.updated(data)
      val snapshot = DeliveryStateSnapshot(state, getDeliverySnapshot)
      saveSnapshot(snapshot)
      println(s"Snapshot saved --> $snapshot")
  }

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
    case AddItemCommand(id, data) => persist(ItemAddedEvent(id, data))(updateState)
    case ConfirmPrinted(deliveryId, id, data) => persist(ItemPrintedEvent(deliveryId, id, data))(updateState)
    case AtLeastOnceDelivery.UnconfirmedWarning(deliveries) => println(s"UnconfirmedWarning $deliveries!!")
    //Passivate
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      println(s"$persistenceId is stopping...")
      context.stop(self)
  }

}

object ListMaintainerActor {

  case class AddItemCommand(id: String, item: String) extends Command

  case class ConfirmPrinted(deliveryId: Long, id: String, item: String) extends Command

  case class ItemAddedEvent(id: String, item: String) extends Event

  case class ItemPrintedEvent(deliveryId: Long, id: String, item: String) extends Event

  case class ListState(items: List[String] = Nil) {
    def updated(item: String): ListState = copy(item :: items)

    def size: Int = items.length

    override def toString: String = items.reverse.toString
  }

  //save state with AtLeastOnceDeliverySnapshot
  case class DeliveryStateSnapshot(exampleState: ListState, alodSnapshot: AtLeastOnceDeliverySnapshot)

  // Sharding Name
  val shardName: String = "List"

  // messageExtractor
  def messageExtractor(numberOfShards: Int): MessageExtractor = ListMaintainer(numberOfShards)

  private case class ListMaintainer(numberOfShards: Int) extends MessageExtractor {

    // id extractor
    override def entityId(message: Any): String = message match {
      case AddItemCommand(id, _) => id.toString
    }

    // shard resolver
    override def shardId(message: Any): String = message match {
      case AddItemCommand(id, _) => (id.hashCode % numberOfShards).toString
    }

    // get message
    override def entityMessage(message: Any): Any = message match {
      case msg@AddItemCommand(_, _) => msg
    }
  }

  def props(target: ActorRef) = Props(new ListMaintainerActor(target))
}

class ListItemPrinterActor extends Actor {

  override def receive = {
    case PrintListItemCommand(deliveryId, id, data) =>
      //      if (data.startsWith("foo")) {
      println(s"CONFIRMING!! $deliveryId for $id was delivered with value $data!")
      sender() ! ConfirmPrinted(deliveryId, id, data)
    //      } else {
    //        println(s"NOT CONFIRMING !! $deliveryId was delivered with value $data!")
    //      }
  }
}

object ListItemPrinterActor {

  case class PrintListItemCommand(deliveryId: Long, id: String, data: String) extends Command

  def props() = Props[ListItemPrinterActor]
}

object ListMaintainerActorMain extends App {

  startup(Seq("2551", "2552"))

  def startup(ports: Seq[String]): Unit = {
    ports.foreach { port =>

      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
        .withFallback(ConfigFactory.load())

      val system = ActorSystem("ClusterSystem", config)

      val listItemPrinterActor = system.actorOf(ListItemPrinterActor.props())

      ClusterSharding(system).start(
        typeName = ListMaintainerActor.shardName,
        entityProps = ListMaintainerActor.props(listItemPrinterActor),
        settings = ClusterShardingSettings(system),
        messageExtractor = ListMaintainerActor.messageExtractor(10)
      )
    }
  }
}

object SendMain extends App {

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2553")
    .withFallback(ConfigFactory.load())

  val system = ActorSystem("ClusterSystem", config)

  val listItemPrinterActor = system.actorOf(ListItemPrinterActor.props())

  ClusterSharding(system).start(
    typeName = ListMaintainerActor.shardName,
    entityProps = ListMaintainerActor.props(listItemPrinterActor),
    settings = ClusterShardingSettings(system),
    messageExtractor = ListMaintainerActor.messageExtractor(10)
  )

  val cluster = Cluster(system)
  val listMaintainerRegion: ActorRef = ClusterSharding(system).shardRegion(ListMaintainerActor.shardName)

  listMaintainerRegion ! AddItemCommand("list1", "foo")
  listMaintainerRegion ! AddItemCommand("list1", "bar")

  listMaintainerRegion ! AddItemCommand("list2", "foo")

  Thread.sleep(10000)

  listMaintainerRegion ! AddItemCommand("list1", "foo2")

  Thread.sleep(1000)


  system.actorOf(Props(new Actor {
    context.watch(listMaintainerRegion)

    def receive = {
      case Terminated(listMaintainerRegion) =>
        println(s"Terminated $listMaintainerRegion")
        cluster.registerOnMemberRemoved(context.system.terminate())
        cluster.leave(cluster.selfAddress)
    }
  }))

  listMaintainerRegion ! ShardRegion.GracefulShutdown
}

object PersistenceQueryMain extends App {

  import akka.NotUsed
  import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
  import akka.persistence.query.{EventEnvelope, PersistenceQuery}
  import akka.stream.ActorMaterializer
  import akka.stream.scaladsl.Source

  val system = ActorSystem("ClusterSystem")

  implicit val mat = ActorMaterializer()(system)
  val queries = PersistenceQuery(system).readJournalFor[CassandraReadJournal](
    CassandraReadJournal.Identifier
  )

  val evts: Source[EventEnvelope, NotUsed] = queries.eventsByPersistenceId("List-list1", 0, Long.MaxValue)

  evts.runForeach { evt => println(s"Event: $evt") }
}
