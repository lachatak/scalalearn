package org.kaloz.homework

import scala.collection.mutable.StringBuilder

class ScalaZip {

  def solution(A: Int, B: Int): Int = {

    def calculate(A: List[Char], B: List[Char], zip: StringBuilder = new StringBuilder): String = {
      if (A.isEmpty) zip appendAll B toString
      else if (B.isEmpty) zip appendAll A toString
      else calculate(A.tail, B.tail, zip append A.head append B.head)
    }

    val result = calculate(A.toString.toList, B.toString.toList)

    if (result.toLong > 100000000L) -1 else result.toInt

  }
}
