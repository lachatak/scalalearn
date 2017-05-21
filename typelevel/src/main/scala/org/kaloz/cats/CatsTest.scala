package org.kaloz.cats

import cats._
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CatsTest extends App {

  Semigroup[Int].combine(1, 2)

  1.combine(1)

  Semigroup[List[Int]].combine(List(1, 2, 3), List(4, 5, 6))

  println(List(1, 2, 3) |+| List(3, 5, 6))

  val l = { (x: Int) => x + 1 } |+| { (x: Int) => x + 2 }
  val d = { (x: Int) => x.toString } |+| { (x: Int) => x.toString }

  println(l(1))
  println(d(2))

  println(Map(1 -> "A", 2 -> "B") |+| Map(2 -> "A", 3 -> "C"))

  println(List(1, 2, 3, 4).foldMap(identity))


  val len: String => Int = _.length
  val lenOption: Option[String] => Option[Int] = Functor[Option].lift(len)

  implicit val futureFunctor: Functor[Future] = new Functor[Future] {
    def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa map f
  }

  val futureOpt = Functor[Future] compose Functor[Option]
  println(futureOpt.map(Future.successful(Some(1)))(_ + 1))

  val listOpt = Functor[List] compose Functor[Option]
  println(listOpt.map(List(Some(1), None, Some(3)))(_ + 1))

  println(List(1, 2, 3, 4).fproduct("a" * _).map(_.swap).toMap)
  println(List(1, 2, 3, 4).as("a"))

  println(Functor[List].imap(List(1, 2, 3, 4))("a" * _)(_.size))


}
