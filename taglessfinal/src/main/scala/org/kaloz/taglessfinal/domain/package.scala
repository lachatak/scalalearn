package org.kaloz.taglessfinal

package object domain {

  abstract class DomainError(val message: String)

  case class InvalidName(name: String) extends DomainError(s"'$name' is not supported!")

  trait Domain

  final case class Name(name: String) extends Domain

  final case class Greeting(greeting: String) extends Domain

}
