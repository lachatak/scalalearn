package org.kaloz

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import cats.implicits._
import org.json4s.{DefaultFormats, FieldSerializer, Formats, Serialization, jackson}
import org.kaloz.taglessfinal.domain.{Domain, DomainError}
import org.kaloz.taglessfinal.infrastructure.{ApiRequest, ApiResponse, Assembler, AssemblerK, Disassembler, DisassemblerK, ErrorResponse}

import scala.concurrent.Future

package object taglessfinal {

  type TestDomainType[A] = Either[DomainError, A]
  type TestInfraType[A] = Either[ErrorResponse, A]

  implicit val assemblerK: AssemblerK[TestDomainType] = new AssemblerK[TestDomainType] {
    override def toDomain[I <: ApiRequest, D <: Domain](from: I)(implicit A: Assembler[I, D]): TestDomainType[D] = A.toDomain(from).toEither
  }

  implicit val disassemblerK: DisassemblerK[TestDomainType, TestInfraType] = new DisassemblerK[TestDomainType, TestInfraType] {
    override def fromDomain[D <: Domain, I <: ApiResponse](from: TestDomainType[D])(implicit D: Disassembler[D, I]): TestInfraType[I] =
      from.bimap(D.fromDomainError, D.fromDomain)
  }

  implicit val serialization: Serialization = jackson.Serialization
  implicit val formats: Formats = DefaultFormats + FieldSerializer[ErrorResponse]()

  implicit def responseMarshaller(implicit ma: ToEntityMarshaller[ApiResponse],
                                  me: ToEntityMarshaller[ErrorResponse]): ToResponseMarshaller[TestInfraType[_]] =
    Marshaller(implicit ec => r =>
      Future.successful(r).flatMap {
        case Right(a) => ma.map(me => HttpResponse(entity = me))(a.asInstanceOf[ApiResponse])
        case Left(e) => me.map(me => HttpResponse(status = StatusCodes.InternalServerError, entity = me))(e)
      })
}
