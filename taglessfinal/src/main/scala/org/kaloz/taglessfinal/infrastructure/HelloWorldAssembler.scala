package org.kaloz.taglessfinal.infrastructure

import cats.Monad
import monix.cats.monixToCatsMonad
import org.kaloz.taglessfinal.domain.{DomainError, Greeting, Name}
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.HelloWorldResponse
import org.kaloz.taglessfinal.main.Main.{DomainExecution, InfraExecution}

abstract class HelloWorldAssembler[F[_] : Monad, G[_]] {

  def assemble(name: String): F[Name] = implicitly[Monad[F]].pure(Name(name))

  def disassemble(greeting: F[Greeting]): G[HelloWorldResponse]
}

case class HelloWorldAssemblerInterpreter() extends HelloWorldAssembler[DomainExecution, InfraExecution] {

  def disassemble(greeting: DomainExecution[Greeting]): InfraExecution[HelloWorldResponse] = {
    val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)

    greeting.bimap(disassembleLeft, g => HelloWorldResponse(g.greeting))
  }
}
