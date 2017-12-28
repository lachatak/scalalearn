package org.kaloz.taglessfinal.infrastructure

import cats.data.EitherT
import monix.cats.monixToCatsMonad
import org.kaloz.taglessfinal.domain.{DomainError, Greeting, Name}
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.HelloWorldResponse
import org.kaloz.taglessfinal.main.Main.{DomainExecution, InfraExecution}

abstract class HelloWorldAssembler[F[_], G[_]] {

  def assemble(name: String): F[Name]

  def disassemble(greeting: F[Greeting]): G[HelloWorldResponse]
}

case class HelloWorldAssemblerImp() extends HelloWorldAssembler[DomainExecution, InfraExecution] {

  def assemble(name: String): DomainExecution[Name] = EitherT.fromEither(Name(name).toEither)

  def disassemble(greeting: DomainExecution[Greeting]): InfraExecution[HelloWorldResponse] = {
    val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)

    greeting.bimap(disassembleLeft, g => HelloWorldResponse(g.greeting))
  }
}
