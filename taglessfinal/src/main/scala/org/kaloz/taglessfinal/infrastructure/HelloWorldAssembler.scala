package org.kaloz.taglessfinal.infrastructure

import cats.data.EitherT
import cats.implicits._
import monix.cats.monixToCatsMonad
import org.kaloz.taglessfinal.domain.{DomainError, Greeting, Name}
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.{HelloWorldRequest, HelloWorldResponse}
import org.kaloz.taglessfinal.main.Main.{DomainTaskExecution, InfraTaskExecution}
import org.kaloz.taglessfinal.main.Main2.{DomainEitherExecution, InfraEitherExecution}

trait HelloWorldAssembler[F[_], G[_]] extends Assembler[F, HelloWorldRequest, Name] with Disassembler[F, G, Greeting, HelloWorldResponse] {

  def toDomain(request: HelloWorldRequest): F[Name]

  def fromDomain(greeting: F[Greeting]): G[HelloWorldResponse]
}

object HelloWorldAssembler {

  def apply[F[_], G[_]](implicit A: HelloWorldAssembler[F, G]) = A

  implicit val helloWorldTaskAssembler: HelloWorldAssembler[DomainTaskExecution, InfraTaskExecution] = new HelloWorldAssembler[DomainTaskExecution, InfraTaskExecution] {
    def toDomain(request: HelloWorldRequest): DomainTaskExecution[Name] = EitherT.fromEither(Name(request.name).toEither)

    def fromDomain(greeting: DomainTaskExecution[Greeting]): InfraTaskExecution[HelloWorldResponse] = {
      val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)

      greeting.bimap(disassembleLeft, g => HelloWorldResponse(g.message))
    }
  }

  implicit val helloWorldEitherAssembler: HelloWorldAssembler[DomainEitherExecution, InfraEitherExecution] = new HelloWorldAssembler[DomainEitherExecution, InfraEitherExecution] {
    def toDomain(request: HelloWorldRequest): DomainEitherExecution[Name] = Name(request.name).toEither

    def fromDomain(greeting: DomainEitherExecution[Greeting]): InfraEitherExecution[HelloWorldResponse] = {
      val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)

      greeting.bimap(disassembleLeft, g => HelloWorldResponse(g.message))
    }
  }

}
