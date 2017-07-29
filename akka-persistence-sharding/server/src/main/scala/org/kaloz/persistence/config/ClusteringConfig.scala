package org.kaloz.persistence.config

import com.typesafe.config.ConfigFactory

object ClusteringConfig {
  private val config = ConfigFactory.load()

  val clusterName = config.getString("clustering.cluster.name")

  val httpPort = config.getInt("clustering.http-port")

  val kafkaIp = config.getString("clustering.kafka-ip")

  val withReceptionist = config.getBoolean("clustering.with-receptionist")
  val receptionistName = config.getString("clustering.receptionist-name")
  val receptionistIp = config.getString("clustering.receptionist-addr")
  val receptionistPort = config.getString("clustering.receptionist-port")

}
