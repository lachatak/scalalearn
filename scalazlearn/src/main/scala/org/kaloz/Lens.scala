package org.kaloz

import scalaz._

object LensTest extends App {

  case class Point(x: Double, y: Double)

  case class Color(r: Byte, g: Byte, b: Byte)

  case class Turtle(
                     position: Point,
                     heading: Double,
                     color: Color)

  val t0 = Turtle(Point(2.0, 3.0), 0.0,
    Color(255.toByte, 255.toByte, 255.toByte))

  val turtlePosition = Lens.lensu[Turtle, Point]((a, value) => a.copy(position = value), _.position)
  val pointX = Lens.lensu[Point, Double]((a, value) => a.copy(x = value), _.x)
  val pointY = Lens.lensu[Point, Double]((a, value) => a.copy(y = value), _.y)
  val turtleHeading = Lens.lensu[Turtle, Double]((a, value) => a.copy(heading = value), _.heading)

  val turtleX = turtlePosition >=> pointX
  println(turtleX.get(t0))

  val t1 = turtleX.set(t0, 5.0)
  val t2 = turtleX.mod(_ + 1.0, t1)

  val incX = turtleX =>= {
    _ + 1.0
  }
  val t3 = incX(t0)

  println(t0)
  println(t1)
  println(t2)
  println(t3)
}
