package org.kaloz.algorithm.triangle

/**
  * Count the number of possible triangles
  * Given an unsorted array of positive integers. Find the number of triangles that can be formed with three different array elements as three sides of triangles. For a triangle to be possible from 3 values, the sum of any two values (or sides) must be greater than the third value (or third side).
  * For example, if the input array is {4, 6, 3, 7}, the output should be 3. There are three triangles possible {3, 4, 6}, {4, 6, 7} and {3, 6, 7}. Note that {3, 4, 7} is not a possible triangle.
  * As another example, consider the array {10, 21, 22, 100, 101, 200, 300}. There can be 6 possible triangles: {10, 21, 22}, {21, 100, 101}, {22, 100, 101}, {10, 100, 101}, {100, 101, 200} and {101, 200, 300}
  */
object Main extends App {
  println(Triangle.numberOfTriangles(Vector(4, 6, 3, 7)))
  println(Triangle.numberOfTriangles(Vector(10, 21, 22, 100, 101, 200, 300)))

  TriangleFindOne.find(Vector(4, 6, 3, 7)).take(10).foreach(println)
  TriangleFindOne.find(Vector(10, 21, 22, 100, 101, 200, 300)).take(10).foreach(println)
}

object Triangle {

  def numberOfTriangles(sides: Vector[Int]): Int = {

    val sortedSides = sides.sorted
    val n = sortedSides.size

    var count = 0

    (0 until n - 2).foreach { i =>
      (i + 1 until n).foreach { j =>

        var k = j + 1
        while (k < n && sortedSides(i) + sortedSides(j) > sortedSides(k))
          k = k + 1

        count = count + k - j - 1
      }
    }

    count
  }
}

object TriangleFindOne {

  type Triangle = (Int, Int, Int)
  type Solutions = Stream[Triangle]

  def find(sides: Vector[Int]): Solutions = {

    val sortedSides = sides.sorted
    val n = sortedSides.size

    val combinations = for {
      i <- Stream.range(0, n - 2)
      j <- Stream.range(i + 1, n)
      k <- Stream.range(j + 1, n)
    } yield (i, j, k)

    combinations.collect{case (i, j, k) if (sortedSides(i) + sortedSides(j) > sortedSides(k)) => (sortedSides(i), sortedSides(j), sortedSides(k))}
  }
}
