package org.kaloz.persistence.main

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

object PersistenceQueryMain extends App {

  import org.kaloz.persistence.config.ClusteringConfig._

  val system = ActorSystem(clusterName)
  val log: LoggingAdapter = Logging.getLogger(system, this)

  implicit val mat = ActorMaterializer()(system)
  val queries = PersistenceQuery(system).readJournalFor[CassandraReadJournal](
    CassandraReadJournal.Identifier
  )

  val evts: Source[EventEnvelope, NotUsed] = queries.eventsByPersistenceId("Payment-list1", 0, Long.MaxValue)

  evts.runForeach { evt => log.info(s"Event: $evt") }
}
