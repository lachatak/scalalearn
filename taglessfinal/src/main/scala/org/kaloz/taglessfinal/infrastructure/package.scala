package org.kaloz.taglessfinal

import org.kaloz.taglessfinal.domain.Domain

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

}
