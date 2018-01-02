package org.kaloz.taglessfinal

import cats.data.EitherT
import monix.eval.Task
import org.kaloz.taglessfinal.domain.{Domain, DomainError, Name, ValidatedDomain}
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.HelloWorldRequest
import org.kaloz.taglessfinal.main.Main.DomainTaskExecution
import cats.implicits._

package object infrastructure {

  trait ApiRequest

  object ApiRequest {

    implicit class ApiRequestSyntax[I <: ApiRequest](request: I) {
      def toDomain[F[_], D <: Domain](implicit A: Assembler[F, I, D]): F[D] = A.toDomain(request)
    }

  }

  trait ApiResponse

  object ApiResponse {

    implicit class ApiResponseSyntax[F[_], D <: Domain](domain: F[D]) {
      def toInfrastructure[G[_], I <: ApiResponse](implicit D: Disassembler[F, G, D, I]): G[I] = D.fromDomain(domain)
    }

  }

  case class ErrorResponse(errorCode: String) extends ApiResponse

  trait Assembler[F[_], I <: ApiRequest, D <: Domain] {
    def toDomain(from: I): F[D]
  }

  trait Disassembler[F[_], G[_], D <: Domain, I <: ApiResponse] {
    def fromDomain(from: F[D]): G[I]
  }



  trait Assembler2[I, D] {
    def toDomain(from: I): D
  }

  trait AssemblerK[F[_]]{
    def toDomain[I, D](from:I)(implicit A:Assembler2[I, D]):F[D]
  }

  object AssemblerK{
    def apply[F[_]](implicit K:AssemblerK[F]) = K

    implicit val domainTaskExecution: AssemblerK[DomainTaskExecution] = new AssemblerK[DomainTaskExecution]{
      override def toDomain[I, D](from: I)(implicit A: Assembler2[I, D]): DomainTaskExecution[D] = EitherT(Task.eval(A.toDomain(from).asRight[DomainError]))
    }
  }

  object Assembler2 {

    def apply[I, D](implicit A:Assembler2[I, D]) = A

    implicit val request2Name: Assembler2[HelloWorldRequest, Name] = (request:HelloWorldRequest) => Name.unsafe(request.name)

    implicit def kindAssembler[F[_], I <:ApiRequest, D <: Domain](implicit K:AssemblerK[F], A:Assembler2[I, D]) = new Assembler2[I, F[D]]{
      override def toDomain(from: I): F[D] = K.toDomain(from)
    }
  }
}
