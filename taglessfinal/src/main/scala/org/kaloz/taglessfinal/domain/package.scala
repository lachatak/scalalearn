package org.kaloz.taglessfinal

import cats.data.Validated
import cats.implicits._
import org.scalameta.data.data

package object domain {

  abstract class DomainError(val message: String)

  case class InvalidName(name: String) extends DomainError(s"'$name' is not valid! Can contain only ${Name.ValidName}")

  case class NotSupportedName(name: String) extends DomainError(s"'$name' is not supported!")

  trait Domain

  @data
  class Name private(val name: String) extends Domain

  object Name {

    val ValidName = "[a-zA-Z]+"

    def apply(name: String): Validated[DomainError, Name] =
      if (name.matches(ValidName)) {
        new Name(name).valid[DomainError]
      } else {
        InvalidName(name).invalid[Name]
      }
  }

  final case class Greeting(greeting: String) extends Domain

}
