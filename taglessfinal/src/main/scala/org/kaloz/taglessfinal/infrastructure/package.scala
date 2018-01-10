package org.kaloz.taglessfinal

import org.kaloz.taglessfinal.domain.{Domain, DomainError, ValidatedDomain}

package object infrastructure {

  trait ApiRequest

  object ApiRequest {

    implicit class ApiRequestSyntax[F[_], I <: ApiRequest](request: I)(implicit K: AssemblerK[F]) {
      def toDomain[D <: Domain](implicit A: Assembler[I, D]): F[D] = K.toDomain(request)
    }

  }

  trait ApiResponse

  case class ErrorResponse(errorCode: String) extends ApiResponse

  object ApiResponse {

    implicit class ApiResponseSyntax[F[_], G[_], D <: Domain](domain: F[D])(implicit K: DisassemblerK[F, G]) {
      def toInfrastructure[I <: ApiResponse](implicit D: Disassembler[D, I]): G[I] = K.fromDomain(domain)
    }

  }

  trait Assembler[I <: ApiRequest, D <: Domain] {
    def toDomain(from: I): ValidatedDomain[D]
  }

  object Assembler {
    def apply[I <: ApiRequest, D <: Domain](implicit A: Assembler[I, D]) = A
  }

  trait AssemblerK[F[_]] {
    def toDomain[I <: ApiRequest, D <: Domain](from: I)(implicit A: Assembler[I, D]): F[D]
  }

  object AssemblerK {
    def apply[F[_]](implicit K: AssemblerK[F]) = K
  }

  trait Disassembler[D <: Domain, I <: ApiResponse] {
    def fromDomain(from: D): I

    def fromDomainError[E <: DomainError](error:E) : ErrorResponse
  }

  object Disassembler {
    def apply[D <: Domain, I <: ApiResponse](implicit D: Disassembler[D, I]) = D
  }

  trait DisassemblerK[F[_], G[_]] {
    def fromDomain[D <: Domain, I <: ApiResponse](from: F[D])(implicit D: Disassembler[D, I]): G[I]
  }

  object DisassemblerK {
    def apply[F[_], G[_]](implicit K: DisassemblerK[F, G]) = K

    //    implicit def instance[F[_, _] :Bifunctor, G[_]]: DisassemblerK[F, G] = new DisassemblerK[F, G] {
    //      override def fromDomain[D <: Domain, I <: ApiResponse](from: F[DomainError, D])(implicit D: Disassembler[D, I]): G[I] = {
    //        val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)
    //
    //        implicitly[Bifunctor[F]].bimap(from)(disassembleLeft, D.fromDomain).asInstanceOf[G[I]]
    //      }
    //    }

  }

}
