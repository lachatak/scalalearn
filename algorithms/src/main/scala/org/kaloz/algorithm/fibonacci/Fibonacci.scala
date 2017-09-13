package org.kaloz.algorithm.fibonacci

import scala.annotation.tailrec

object Main extends App {
  FibonacciStreams.fibonacci(10).foreach(println)
  println("------")
  FibonacciDP1.fibonacci(10).foreach(println)
  println("------")
  FibonacciDP2.fibonacci(10).foreach(println)
}

object FibonacciStreams {

  val fibs: Stream[BigInt] = BigInt(0) #:: BigInt(1) #:: fibs.zip(fibs.tail).collect { case (a, b) => a + b }

  def fibonacci(n: Int): Stream[BigInt] = {
    fibs.take(10)
  }
}

object FibonacciDP1 {

  def fibonacci(n: Int): List[BigInt] = {
    @tailrec
    def calculate(state: List[BigInt] = List(1, 0), n: Int): List[BigInt] = {
      if (n == 0) {
        state
      } else {
        calculate(state.take(2).sum :: state, n - 1)
      }
    }

    calculate(n = n - 2 )
  }
}

object FibonacciDP2 {

  def fibonacci(n: Int): Vector[BigInt] = {
    @tailrec
    def calculate(state: Vector[BigInt] = Vector(0, 1), n: Int): Vector[BigInt] = {
      if (n == 0) {
        state
      } else {
        calculate(state :+ state.drop(state.size - 2).sum, n - 1)
      }
    }

    calculate(n = n)
  }
}
