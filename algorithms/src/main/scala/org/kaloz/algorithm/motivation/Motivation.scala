package org.kaloz.algorithm.motivation

object Main extends App {
  println(MotivationGreedy.optimise(Vector(2, 3, 5, 1, 4)))
}

object MotivationGreedy {
  def optimise(originalPrices: Vector[Int]): Int = {

    val n = originalPrices.size
    val cache = collection.mutable.Map.empty[(Int, Int), Int]

    def calculate(be: Int, en: Int): Int = {
      println(s"-> $be - $en")
      if (be > en) {
        0
      } else {
        cache.get(be, en).fold {
          println(s"calculate $be - $en")
          val year = n - (en - be + 1) + 1
          val max = Math.max(calculate(be + 1, en) + year * originalPrices(be), calculate(be, en - 1) + year * originalPrices(en))
          cache += (be, en) -> max
          max
        }(identity)
      }
    }

    calculate(0, n - 1)
  }
}
