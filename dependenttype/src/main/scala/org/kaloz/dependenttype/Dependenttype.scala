package org.kaloz.dependenttype

import shapeless.ops.hlist.Length
import shapeless.{Generic, HList, Nat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._

object TypeclassApp extends App {

  trait Printer[T] {
    type A

    def print(t: T): A
  }

  implicit val int = new Printer[Int] {
    type A = String

    def print(i: Int) = i.toString
  }

  implicit val string = new Printer[String] {
    type A = Boolean

    def print(i: String) = i.length % 2 == 0
  }

  def foo[T](t: T)(implicit p: Printer[T]): p.A = p.print(t)

  val res1: String = foo(3)
  val res2: Boolean = foo("test2")

  println(s"res: ${res1}")
  println(s"res: ${res2}")
}

object TypeclassResolverApp extends App {

  trait Printer[T] {
    def print(t: T): String
  }

  object Printer {
    implicit val intPrinter: Printer[Int] = new Printer[Int] {
      def print(i: Int) = s"$i: Int"
    }

    implicit val stringPrinter: Printer[String] = new Printer[String] {
      def print(i: String) = s"$i: String"
    }

    implicit def optionPrinter[V](implicit pv: Printer[V]): Printer[Option[V]] =
      new Printer[Option[V]] {
        def print(ov: Option[V]) = ov match {
          case None => "None"
          case Some(v) => s"Option[${pv.print(v)}]"
        }
      }

    implicit def listPrinter[V](implicit pv: Printer[V]): Printer[List[V]] =
      new Printer[List[V]] {
        def print(ov: List[V]) = ov match {
          case Nil => "Nil"
          case l: List[V] => s"List[${l.map(pv.print).mkString(", ")}]"
        }
      }
  }

  def print[T](t: T)(implicit p: Printer[T]) = p.print(t)

  val res1 = print(Option(List(1, 3, 6)))
  val res2 = print(Option(List("1", "2", "2")))
  println(s"res: ${res1}")
  println(s"res: ${res2}")
}

object AuxPatternApp extends App {

  trait Foo[A] {
    type B

    def value: B
  }

  type Aux[A0, B0] = Foo[A0] {type B = B0}

  implicit val fi = new Foo[Int] {
    type B = String
    val value = "Foo"
  }
  implicit val fs = new Foo[String] {
    type B = Int
    val value = 3
  }

  def foo[T, R](t: T)(implicit f: Aux[T, R], m: Monoid[R]): R = m.zero

  val res1: String = foo(2)
  val res2: Int = foo("")

  println(s"monoid for 2 is $res1}")
  println(s"""monoid for "" is $res2}""")

  def length[T, R <: HList](t: T)(implicit g: Generic.Aux[T, R], l: Length[R]): l.Out = l()

  case class Clazz(i: Int, s: String, b: Boolean)

  val clazz = Clazz(1, "", false)
  val res = length(clazz)

  println(s"res: ${Nat.toInt(res)}")
}

object IsFutureTypeclassApp extends App {

  trait IsFuture[F] {
    type T

    def apply(f: F): Future[T]
  }

  object IsFuture {
    def apply[F](implicit isf: IsFuture[F]) = isf


    implicit val stringF = new IsFuture[String] {
      type T = Boolean

      override def apply(f: String): Future[Boolean] = Future {
        f.length % 2 == 0
      }
    }

    implicit val intF = new IsFuture[Int] {
      type T = String

      override def apply(f: Int): Future[String] = Future {
        s"string: $f"
      }
    }

    implicit def mk[A](implicit pv: IsFuture[A]) = new IsFuture[Future[A]] {
      type T = pv.T

      def apply(f: Future[A]): Future[pv.T] = f.flatMap(pv(_))
    }

  }

  def logResult[Thing](thing: Thing)(implicit isf: IsFuture[Thing]): Future[isf.T] =
    isf(thing).map { x =>
      println(s"I got a result of $x")
      x
    }

  logResult(3)
  logResult(Future.successful("3"))

  Thread.sleep(1000)
}

object MapZeroApp extends App {

  trait Apart[F] {
    type T
    type W[X]

    def apply(f: F): W[T]
  }

  object Apart {
    def apply[F](implicit apart: Apart[F]) = apart

    type Aux[FA, A, F[_]] = Apart[FA] {type T = A; type W[X] = F[X]}

    implicit def mk[F[_], A]: Aux[F[A], A, F] = new Apart[F[A]] {
      type T = A
      type W[X] = F[X]

      def apply(f: F[A]): W[T] = f
    }
  }

  def mapZero[Thing, F[_], A](thing: Thing)(implicit apart: Apart.Aux[Thing, A, F], f: Functor[F], m: Monoid[A]): F[A] =
    f.map(apart(thing))(_ => m.zero)

  println(mapZero(List(2, 3, 4, 5)))
  println(mapZero(Option(2)))
}
