package org.kaloz.simulacrum

import Semigroup.ops._

object Test extends App {

  implicit val intAdditionSemigroup: Semigroup[Int] = new Semigroup[Int] {
    override def append(x: Int, y: Int): Int = x + y
  }

  println( 10 |+| 5 )
}
