package org.kaloz.eff

import cats.data._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import org.atnos.eff.all._
import org.atnos.eff.syntax.addon.monix.task._
import org.atnos.eff.syntax.all._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object EffTest extends App {

  type _eitherString[R] = Either[String, ?] |= R
  type _flowId[R] = Reader[String, ?] |= R

  def test[R: _eitherString : _task : _flowId](value : Int): Eff[R, Int] = for {
    x <- method1(5)
    y <- either2(value)
  } yield x + y

  def method1[R](int: Int): Eff[R, Int] = Eff.pure(int)

  def either2[R: _eitherString : _task : _flowId](int: Int): Eff[R, Int] = for {
    id <- ask[R, String]
    v <- if (int == 0) left(s"error in $id") else fromTask(Task(int))
  } yield v

  type FxStack = Fx.fx3[Either[String, ?], Task, Reader[String, ?]]

  val resultRunStateRunEither: Future[Either[String, Int]] = test[FxStack](5).runReader("ID").runEither.runAsync.runAsync

  val result = Await.result(resultRunStateRunEither, 5 second)
  println(result)

}
