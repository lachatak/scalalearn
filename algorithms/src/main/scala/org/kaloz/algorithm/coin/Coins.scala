package org.kaloz.algorithm.coin

import scala.annotation.tailrec

object Main extends App {
  println(CoinsGreedy.split(16, List(5, 3, 1)))
  println(CoinsBacktrack.split(16, List(5, 2)))
  CoinsBacktrackStream.split(16, List(5, 2)).take(2).foreach(println)
}

object CoinsGreedy {
  def split(amount: Int, coins: List[Int]): List[Int] = {
    @tailrec
    def calculate(amount: Int, coins: List[Int], result: List[Int] = List.empty): List[Int] = (amount, coins) match {
      case (0, _) => result
      case (a, x :: _) if (a >= x) => calculate(a - x, coins, x :: result)
      case (a, _ :: xs) => calculate(a, xs, result)
    }

    calculate(amount, coins)
  }
}

object CoinsBacktrack {
  def split(amount: Int, coins: List[Int]): List[List[Int]] = {
    def calculate(amount: Int, coins: List[Int], result: List[Int] = List.empty): List[List[Int]] = (amount, coins) match {
      case (a, _) if (a < 0) => List.empty
      case (_, Nil) => List.empty
      case (0, _) => List(result)
      case (a, x :: xs) => calculate(a - x, coins, x :: result) ::: calculate(a, xs, result)
    }

    calculate(amount, coins)
  }
}

object CoinsBacktrackStream {
  def split(amount: Int, coins: List[Int]): Stream[List[Int]] = {
    def calculate(amount: Int, coins: List[Int], result: List[Int] = List.empty): Stream[List[Int]] = (amount, coins) match {
      case (a, _) if (a < 0) => Stream.empty
      case (_, Nil) => Stream.empty
      case (0, _) => Stream(result)
      case (a, x :: xs) => calculate(a - x, coins, x :: result) #::: calculate(a, xs, result)
    }

    calculate(amount, coins)
  }
}