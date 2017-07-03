package org.kaloz.persistence

import java.util.UUID

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, ReceiveTimeout, Terminated}
import akka.cluster.Cluster
import akka.cluster.sharding.ShardRegion.{MessageExtractor, Passivate}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.event.{Logging, LoggingAdapter}
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import akka.persistence.{AtLeastOnceDelivery, PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.typesafe.config.ConfigFactory
import org.kaloz.persistence.ListItemPrinterActor.PrintListItemCommand
import org.kaloz.persistence.ListMaintainerActor.{AddItemCommand, AddItemInitiatedEvent, DeliveryStateSnapshot, ItemAddedEvent, ListState, PrintConfirmedEvent}

import scala.concurrent.duration._

sealed trait Command

sealed trait Event

class ListMaintainerActor(listItemPrinterActor: ActorRef) extends PersistentActor with AtLeastOnceDelivery with ActorLogging {

  context.setReceiveTimeout(5.seconds)

  override def persistenceId = "List-" + self.path.name

  var state = ListState()

  def updateState(event: Event): Unit = event match {
    case AddItemInitiatedEvent(commandId, id, data) =>
      state = state.processingCommand(commandId)
      deliver(listItemPrinterActor.path)(deliveryId => PrintListItemCommand(deliveryId, id, data))
    case l@ItemAddedEvent(deliveryId, id, data) =>
      confirmDelivery(deliveryId)
      log.info(s"ListItemPrintedEvent --> $l")
      state = state.updated(data)
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
    case AddItemCommand(commandId, id, data) =>
      if (!state.isCommandIdProcessed(commandId)) {
        persist(AddItemInitiatedEvent(commandId, id, data))(updateState)
      }
      else {
        log.info(s"$commandId is processed!")
      }
    case PrintConfirmedEvent(deliveryId, id, data) => persist(ItemAddedEvent(deliveryId, id, data))(updateState)
    case AtLeastOnceDelivery.UnconfirmedWarning(deliveries) => log.info(s"UnconfirmedWarning $deliveries!!")
    //Passivate
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      log.info(s"$persistenceId is stopping...")
      context.stop(self)
  }

}

object ListMaintainerActor {

  case class AddItemCommand(commandId: UUID, id: String, item: String) extends Command

  case class AddItemInitiatedEvent(commandId: UUID, id: String, item: String) extends Event

  case class PrintConfirmedEvent(deliveryId: Long, id: String, item: String) extends Event

  case class ItemAddedEvent(deliveryId: Long, id: String, item: String) extends Event

  case class ListState(commandIds: List[UUID] = Nil, items: List[String] = Nil) {

    def updated(item: String): ListState = copy(items = item :: items)

    def processingCommand(commandId: UUID): ListState = copy(commandIds = commandId :: commandIds)

    def isCommandIdProcessed(commandId: UUID) = commandIds.contains(commandId)

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
      case AddItemCommand(_, id, _) => id.toString
    }

    // shard resolver
    override def shardId(message: Any): String = message match {
      case AddItemCommand(_, id, _) => (id.hashCode % numberOfShards).toString
    }

    // get message
    override def entityMessage(message: Any): Any = message match {
      case msg@AddItemCommand(_, _, _) => msg
    }
  }

  def props(target: ActorRef) = Props(new ListMaintainerActor(target))
}

class ListItemPrinterActor extends Actor with ActorLogging {

  override def receive = {
    case PrintListItemCommand(deliveryId, id, data) =>
      //      if (data.startsWith("foo")) {
      log.info(s"CONFIRMING!! $deliveryId for $id was delivered with value $data!")
      sender() ! PrintConfirmedEvent(deliveryId, id, data)
    //      } else {
    //        log.info(s"NOT CONFIRMING !! $deliveryId was delivered with value $data!")
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

  listMaintainerRegion ! AddItemCommand(UUID.randomUUID(), "list1", "foo")
  listMaintainerRegion ! AddItemCommand(UUID.randomUUID(), "list1", "bar")

  listMaintainerRegion ! AddItemCommand(UUID.randomUUID(), "list2", "foo")

  Thread.sleep(10000)

  listMaintainerRegion ! AddItemCommand(UUID.randomUUID(), "list1", "foo2")

  Thread.sleep(1000)


  system.actorOf(Props(new Actor with ActorLogging {
    context.watch(listMaintainerRegion)

    def receive = {
      case Terminated(listMaintainerRegion) =>
        log.info(s"Terminated $listMaintainerRegion")
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
  val log: LoggingAdapter = Logging.getLogger(system, this)

  implicit val mat = ActorMaterializer()(system)
  val queries = PersistenceQuery(system).readJournalFor[CassandraReadJournal](
    CassandraReadJournal.Identifier
  )

  val evts: Source[EventEnvelope, NotUsed] = queries.eventsByPersistenceId("List-list1", 0, Long.MaxValue)

  evts.runForeach { evt => log.info(s"Event: $evt") }
}