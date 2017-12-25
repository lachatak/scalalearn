package org.kaloz.taglessfinal.infrastructure

import cats.Monad
import org.kaloz.taglessfinal.domain.Greeting
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.HelloWorldResponse

case class HelloWorldAssembler[F[_] : Monad]() {

  def disassemble(greeting: Greeting): F[HelloWorldResponse] =
    implicitly[Monad[F]].pure(HelloWorldResponse(greeting.greeting))
}
