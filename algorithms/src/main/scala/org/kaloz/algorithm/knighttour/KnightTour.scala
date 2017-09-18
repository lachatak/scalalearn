package org.kaloz.algorithm.knighttour

object Main extends App {
  Knights.knightSteps(8).take(100).foreach(Knights.printSolution)
  KnightsArray.knightSteps(8).take(100).foreach(KnightsArray.printSolution)
}

object Knights {

  type Position = (Int, Int)
  type Steps = List[Position]
  type Solutions = Stream[Steps]

  def knightSteps(size: Int): Solutions = {

    val jumps = List((-1, -2), (-2, -1), (1, -2), (2, -1), (-2, 1), (-1, 2), (1, 2), (2, 1))

    val startPositions: Stream[(Int, Int)] = Stream.from(0).takeWhile(_ < size * size).map(i => (i / size + 1, i % size + 1))

    def isAvailable(position: Position, steps: Steps): Boolean = steps.forall(_ != position)

    def generateNewPositions(from: Position, exists: Steps): Steps = jumps
      .collect { case (x, y) => (from._1 + x, from._2 + y) }
      .filter(p => p._1 >= 1 && p._1 <= size && p._2 >= 1 && p._2 <= size)
      .filter(isAvailable(_, exists))

    def calculateSteps(positions: Steps = List.empty, steps: Steps = List.empty): Solutions = {
      (positions, steps) match {
        case (_, s) if (s.size == size * size) => Stream(s.reverse)
        case (Nil, _) => Stream(Nil)
        case (x :: Nil, s) => calculateSteps(generateNewPositions(x, s), x :: s)
        case (x :: xs, s) => calculateSteps(generateNewPositions(x, s), x :: s) #::: calculateSteps(xs, x :: s)
      }
    }

    startPositions.flatMap(s => calculateSteps(List(s))).filter(_ != Nil)
  }

  def printSolution(steps: Steps): Unit = {
    val size = Math.sqrt(steps.size).toInt
    val board = Array.ofDim[Int](size, size)
    steps.zip(0 to (steps.size - 1)).foreach(s => board(s._1._1 - 1)(s._1._2 - 1) = s._2)

    println(board.map(_.mkString(",")).mkString("\n"))
  }
}


object KnightsArray {

  type Position = (Int, Int)
  type Steps = List[Position]
  type Board = Vector[Vector[Int]]
  type Solutions = Stream[Board]

  def knightSteps(size: Int): Solutions = {

    val jumps = List((-1, -2), (-2, -1), (1, -2), (2, -1), (-2, 1), (-1, 2), (1, 2), (2, 1))

    val startPositions: Stream[(Int, Int)] = Stream.from(0).takeWhile(_ < size * size).map(i => (i / size, i % size))

    def generateNewPositions(from: Position, board: Board): Steps = jumps
      .collect { case (x, y) => (from._1 + x, from._2 + y) }
      .filter(p => p._1 >= 0 && p._1 <= size - 1 && p._2 >= 0 && p._2 <= size - 1 && board(p._1)(p._2) == -1)

    def calculateSteps(positions: Steps = List.empty, board: Board = Vector.fill(size, size)(-1), steps: Int = 0): Solutions = {
      def updateBoard(p: Position, value: Int): Board =
        board.updated(p._1, board(p._1).updated(p._2, value))

      (positions, steps) match {
        case (_, s) if (s == size * size) => Stream(board)
        case (Nil, _) => Stream(Vector.empty)
        case (x :: Nil, s) =>
          calculateSteps(generateNewPositions(x, board), updateBoard(x, s), s + 1)
        case (x :: xs, s) =>
          calculateSteps(generateNewPositions(x, board), updateBoard(x, s), s + 1) #::: calculateSteps(xs, updateBoard(x, s), s + 1)
      }
    }

    startPositions.flatMap(s => calculateSteps(List(s))).filter(_ != Vector.empty)
  }

  def printSolution(board: Board): Unit = {
    println(board.map(_.mkString(",")).mkString("\n"))
  }
}
