package org.kaloz.algorithm.nqueen


object Main extends App {
  NQueen.placeQueen(8).take(2).foreach(NQueen.printSolution)
}

object NQueen {

  type Queen = (Int, Int)
  type Queens = List[Queen]
  type Solutions = Stream[Queens]

  def placeQueen(size: Int): Solutions = {

    def place(n: Int): Solutions = {

      def isSafe(queen: Queen, queens: Queens): Boolean = {
        def conflict(q1: Queen, q2: Queen): Boolean = {
          q1._1 == q2._1 ||
            q1._2 == q2._2 ||
            (q2._1 - q1._1).abs == (q2._2 - q1._2).abs
        }

        queens.forall(!conflict(_, queen))
      }

      n match {
        case 0 => Stream(Nil)
        case _ => for {
          queens <- place(n - 1)
          j <- 1 to size
          queen = (n, j)
          if (isSafe(queen, queens))
        } yield queen :: queens
      }
    }

    place(size)
  }

  def printSolution(queens: Queens): Unit = {
    println(queens)
    for (queen <- queens.reverse; x <- 1 to queens.size) {
      if (queen._2 == x) print("Q ") else print(". ")
      if (x == queens.size) println()
    }
  }
}
