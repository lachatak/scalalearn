package org.kaloz.persistence.config

import com.typesafe.config.ConfigFactory

object ClusteringConfig {
  private val config = ConfigFactory.load()

  val clusterName = config.getString("clustering.cluster.name")
  val port = config.getInt("clustering.port")

}
