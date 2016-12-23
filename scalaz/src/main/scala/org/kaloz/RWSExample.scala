package org.kaloz

import scalaz.Scalaz._
import scalaz._

object RWSExample extends App {

  case class Config(port: Int)

  type Work[S, A] = ReaderWriterState[Config, List[String], S, A]

  def log[R, S](msg: String): Work[S, Unit] =
    ReaderWriterState {
      (r, s) => (msg.format(r, s) :: Nil, (), s)
    }

  def invokeService: Work[Int, Int] =

    ReaderWriterState {
      (cfg, invocationCount) => (List("Invoking service with port " + cfg.port), scala.util.Random.nextInt(100), invocationCount + 1)
    }

  val program: Work[Int, Int] = for {
    _ <- log("Start - r: %s, s: %s")
    res <- invokeService
    _ <- log("Between - r: %s, s: %s")
    _ <- invokeService
    _ <- log("Done - r: %s, s: %s")
  } yield res

  def run[S,A](work : Work[S, A])(implicit S: Monoid[S]) = work run(Config(443), S.zero)

  val (logMessages, result, invocationCount) = run(program)
  println("Result: " + result)
  println("Service invocations: " + invocationCount)
  println("Log: %n%s".format(logMessages.mkString("\t", "%n\t".format(), "")))
}
