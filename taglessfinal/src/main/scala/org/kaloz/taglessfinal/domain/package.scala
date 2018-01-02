package org.kaloz.taglessfinal

import cats.data.Validated
import cats.implicits._
import org.scalameta.data.data

package object domain {

  sealed trait Domain

  sealed abstract class DomainError(val message: String) extends Domain

  case class InvalidName(name: String) extends DomainError(s"'$name' is not valid! Can contain only ${Name.ValidName}")

  case class NotSupportedName(name: String) extends DomainError(s"'$name' is not supported!")

  type ValidatedDomain[D] = Validated[DomainError, D]

  @data
  class Name private(val name: String) extends Domain

  object Name {

    val ValidName = "[a-zA-Z]+"

    def apply(name: String): ValidatedDomain[Name] =
      if (name.matches(ValidName)) {
        new Name(name).valid[DomainError]
      } else {
        InvalidName(name).invalid[Name]
      }

    def unsafe(name: String): Name = new Name(name)
  }

  case class Greeting(message: String) extends Domain

}
