package org.kaloz.taglessfinal

import cats.data.EitherT
import cats.implicits._
import monix.cats.monixToCatsFunctor
import monix.eval.Task
import org.kaloz.taglessfinal.domain.{Domain, DomainError, ValidatedDomain}
import org.kaloz.taglessfinal.main.Main.{DomainTaskExecution, InfraTaskExecution}
import org.kaloz.taglessfinal.main.Main2.{DomainEitherExecution, InfraEitherExecution}

package object infrastructure {

  trait ApiRequest

  object ApiRequest {

    implicit class ApiRequestSyntax[F[_], I <: ApiRequest](request: I)(implicit K: AssemblerK[F]) {
      def toDomain[D <: Domain](implicit A: Assembler[I, D]): F[D] = K.toDomain[I, D](request)
    }

  }

  trait ApiResponse

  object ApiResponse {

    implicit class ApiResponseSyntax[F[_], G[_], D <: Domain](domain: F[D])(implicit K: DisassemblerK[F, G]) {
      def toInfrastructure[I <: ApiResponse](implicit D: Disassembler[D, I]): G[I] = K.fromDomain(domain)
    }

  }

  case class ErrorResponse(errorCode: String) extends ApiResponse

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

    implicit val domainTaskExecution: AssemblerK[DomainTaskExecution] = new AssemblerK[DomainTaskExecution] {
      override def toDomain[I <: ApiRequest, D <: Domain](from: I)(implicit A: Assembler[I, D]): DomainTaskExecution[D] = EitherT(Task.eval(A.toDomain(from).toEither))
    }

    implicit val domainEitherExecution: AssemblerK[DomainEitherExecution] = new AssemblerK[DomainEitherExecution] {
      override def toDomain[I <: ApiRequest, D <: Domain](from: I)(implicit A: Assembler[I, D]): DomainEitherExecution[D] = A.toDomain(from).toEither
    }
  }

  trait Disassembler[D <: Domain, I <: ApiResponse] {
    def fromDomain(from: D): I
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

    implicit val domainTaskExecution: DisassemblerK[DomainTaskExecution, InfraTaskExecution] = new DisassemblerK[DomainTaskExecution, InfraTaskExecution] {
      override def fromDomain[D <: Domain, I <: ApiResponse](from: DomainTaskExecution[D])(implicit D: Disassembler[D, I]): InfraTaskExecution[I] = {
        val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)

        from.bimap(disassembleLeft, D.fromDomain)
      }
    }

    implicit val domainEitherExecution: DisassemblerK[DomainEitherExecution, InfraEitherExecution] = new DisassemblerK[DomainEitherExecution, InfraEitherExecution] {
      override def fromDomain[D <: Domain, I <: ApiResponse](from: DomainEitherExecution[D])(implicit D: Disassembler[D, I]): InfraEitherExecution[I] = {
        val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)

        from.bimap(disassembleLeft, D.fromDomain)
      }
    }
  }

}
