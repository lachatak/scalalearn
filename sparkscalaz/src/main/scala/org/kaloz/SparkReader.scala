package org.kaloz

import org.apache.spark.{SparkConf, SparkContext}

import scalaz.Reader

object SparkReader extends App {

  type Work[A] = Reader[SparkContext, A]

  println(runWithSpark(loadSummaryByCurrency()))

  def loadSummaryByCurrency(exchangerates: String = "exchangerates.csv", transactions: String = "transactions.csv", currency: String = "GBP"): Work[Map[String, BigDecimal]] =
    for {
      r <- rates(exchangerates, currency)
      s <- summaryByCurrency(transactions, currency, r)
    } yield s

  def rates(exchangerates: String, currency: String): Work[Map[String, BigDecimal]] =
    Reader(sc =>
      sc.textFile(exchangerates)
        .map(_.split(",").map(_.trim.toUpperCase))
        .collect { case Array(from, to, amount) if (currency == to) => (from -> BigDecimal(amount))}
        .collectAsMap()
        .toMap
    )

  def summaryByCurrency(transactions: String, currency: String, rates: Map[String, BigDecimal]): Work[Map[String, BigDecimal]] =
    Reader(sc =>
      sc.textFile(transactions)
        .map(_.split(",").map(_.trim.toUpperCase))
        .map { case Array(partner, to, amount) => (partner, rates.getOrElse(to, BigDecimal(0)) * BigDecimal(amount))}
        .reduceByKey(_ + _)
        .collectAsMap()
        .toMap
    )

  def runWithSpark[T](work: Work[T]): T = {
    val sparkConf: SparkConf = new SparkConf()
      .setAppName("reader")
      .setMaster("local[8]")
      .setJars(SparkContext.jarOfClass(this.getClass).toSeq)

    val sc = new SparkContext(sparkConf)

    try {
      work.run(sc)
    } finally {
      sc.stop
    }
  }
}
