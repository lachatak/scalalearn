package org.kaloz.homework

import org.scalacheck.Prop._
import org.scalacheck._

object ZipPropertyTest extends Properties("zip") {

  val solution = new ScalaZip

  val n = Gen.chooseNum(0, 100000000)
  val nonZero = Gen.chooseNum(1, 100000000)
  val zero = Gen.const(0)

  property("ends") = Prop.forAll(n, n) {
    (n, m) =>
      classify(n.toString.length != m.toString.length, "not equal") {
        val result = solution.solution(n, m)
        if ((n.toString + m.toString).toLong <= 100000000) {
          if (n.toString.length > m.toString.length) result.toString endsWith n.toString.substring(m.toString.length)
          else result.toString endsWith m.toString.substring(n.toString.length)
        } else {
          result == -1
        }
      }
  }

  property("position") = Prop.forAll(nonZero, n) {
    (n, m) =>
      classify(n < 1000 || m < 1000, "short") {
        classify(n > 1000 || m > 1000, "big") {
          (n.toString.length >= 4 && m.toString.length >= 4) ==> {
            val result = solution.solution(n, m)
            if ((n.toString + m.toString).toLong <= 100000000) {
              result.toString.charAt(0) == n.toString.charAt(0) && result.toString.charAt(1) == m.toString.charAt(0) && result.toString.charAt(4) == n.toString.charAt(2) && result.toString.charAt(5) == m.toString.charAt(2)
            } else {
              result == -1
            }
          }
        }
      }
  }

  property("first zero") = Prop.forAll(zero, n) {
    (n, m) =>
      val result = solution.solution(n, m)
      result == m
  }

  property("zero zero") = Prop.forAll(zero, zero) {
    (n, m) =>
      val result = solution.solution(n, m)
      result == 0
  }

  property("bigger than 100.000.000") = Prop.forAll(n, n) {
    (n, m) =>
      classify((n.toString + m.toString).toLong > 100000000, "big") {
        val result = solution.solution(n, m)
        if ((n.toString + m.toString).toLong > 100000000) result == -1
        else result > -1
      }
  }

}