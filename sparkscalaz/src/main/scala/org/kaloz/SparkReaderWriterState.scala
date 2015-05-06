package org.kaloz

import org.apache.spark.{SparkConf, SparkContext}

import scalaz.Scalaz._
import scalaz._

object SparkReaderWriterState extends App {

  type Work[S, A] = ReaderWriterState[SparkContext, List[String], S, A]

  val (logMessages, result, state) = runWithSpark(loadSummaryByCurrency())
  println(s"Result: $result")
  println(s"State: $state")
  println("Log: %n%s".format(logMessages.mkString("\t", "%n\t".format(), "")))

  def loadSummaryByCurrency(exchangerates: String = "exchangerates.csv", transactions: String = "transactions.csv", currency: String = "GBP", partner: String = "Unlimited ltd."): Work[Map[String, BigDecimal], BigDecimal] =
    for {
      _ <- log(s"Loading exchangerates from $exchangerates file!")
      r <- rates(exchangerates, currency)
      _ <- log(s"Calculating transaction summary from $transactions file!")
      _ <- summaryByCurrency(transactions, currency, r)
      _ <- log(s"Process done!")
      s <- summaryByPartner(partner)
    } yield s

  def rates[S](exchangerates: String, currency: String): Work[S, Map[String, BigDecimal]] =
    ReaderWriterState((sc, state) => {
      val rates = sc.textFile(exchangerates)
        .map(_.split(",").map(_.trim.toUpperCase))
        .collect { case Array(from, to, amount) if (currency == to) => (from -> BigDecimal(amount))}
        .collectAsMap()
        .toMap
      (s"${rates.size} exchangerate(s) has been loaded.." :: Nil, rates, state)
    })

  def summaryByCurrency(transactions: String, currency: String, rates: Map[String, BigDecimal]): Work[Map[String, BigDecimal], Map[String, BigDecimal]] =
    ReaderWriterState((sc, state) => {
      val result = sc.textFile(transactions)
        .map(_.split(",").map(_.trim.toUpperCase))
        .map { case Array(partner, to, amount) => (partner, rates.getOrElse(to, BigDecimal(0)) * BigDecimal(amount))}
        .reduceByKey(_ + _)
        .collectAsMap()
        .toMap
      (s"${result.size} partner(s) has been loaded.." :: Nil, result, result)
    })

  def summaryByPartner(partner: String): Work[Map[String, BigDecimal], BigDecimal] =
    ReaderWriterState((sc, state) => {
      val result = state(partner.toUpperCase)
      (s"Result for $partner has been fetched -> $result" :: Nil, result, state)
    })

  def log(message: String): Work[Map[String, BigDecimal], Unit] =
    ReaderWriterState((sc, state) => (message :: Nil, (), state))

  def runWithSpark[S, A](work: Work[S, A])(implicit S: Monoid[S]): (List[String], A, S) = {

    val sparkConf: SparkConf = new SparkConf()
      .setAppName("readerWriterState")
      .setMaster("local[8]")
      .setJars(SparkContext.jarOfClass(this.getClass).toSeq)

    val sc = new SparkContext(sparkConf)

    try {
      work.run(sc, S.zero)
    } finally {
      sc.stop
    }
  }
}
