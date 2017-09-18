package org.kaloz.algorithm.motivation

object Main extends App {
  println(Motivation.optimise(Vector(2, 3, 5, 1, 4)))
}

object Motivation {
  def optimise(originalPrices: Vector[Int]): (List[(Int, Int)], Int) = {

    val n = originalPrices.size
    val cache = collection.mutable.Map.empty[(Int, Int), List[Int]]

    def calculate(be: Int, en: Int): List[Int] = {
      println(s"-> $be - $en")
      if (be > en) {
        Nil
      } else {
        cache.get(be, en).fold {
          println(s"calculate $be - $en")
          val year = n - (en - be + 1) + 1
          val headValueAddedList = year * originalPrices(be) :: calculate(be + 1, en)
          val tailValueAddedList = year * originalPrices(en) :: calculate(be, en - 1)
          val maxList = if (headValueAddedList.sum >= tailValueAddedList.sum) {
            headValueAddedList
          } else {
            tailValueAddedList
          }
          cache += (be, en) -> maxList
          maxList
        }(identity)
      }
    }

    val selectedItemValuesPerYear = calculate(0, n - 1)
    val result = selectedItemValuesPerYear.zip(selectedItemValuesPerYear.zipWithIndex.collect { case (value, index) => value / (index + 1) })

    (result, selectedItemValuesPerYear.sum)
  }
}
