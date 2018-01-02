package org.kaloz.taglessfinal.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import cats.data.EitherT
import cats.implicits._
import monix.cats.monixToCatsMonad
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.{DefaultFormats, FieldSerializer, Formats, Serialization, jackson}
import org.kaloz.taglessfinal.domain.{Domain, DomainError, HelloWorldService}
import org.kaloz.taglessfinal.infrastructure.driving.api.{HelloWorldApi, HelloWorldRestService}
import org.kaloz.taglessfinal.infrastructure.driving.{ApiRequest, ApiResponse, Assembler, AssemblerK, Disassembler, DisassemblerK, ErrorResponse}

import scala.concurrent.Future

object EitherTTaskMain extends App {

  implicit val actorSystem = ActorSystem("hello-world-eitherT-task")
  implicit val materializer = ActorMaterializer()
  implicit val scheduler: Scheduler = Scheduler(actorSystem.dispatcher)

  type DomainTaskExecution[A] = EitherT[Task, DomainError, A]
  type InfraTaskExecution[A] = EitherT[Task, ErrorResponse, A]

  implicit val serialization: Serialization = jackson.Serialization
  implicit val formats: Formats = DefaultFormats + FieldSerializer[DomainError]()

  import de.heikoseeberger.akkahttpjson4s.Json4sSupport.marshaller

  implicit def eitherTTaskMarshaller(implicit ma: ToEntityMarshaller[ApiResponse],
                                     me: ToEntityMarshaller[ErrorResponse],
                                     scheduler: Scheduler): ToResponseMarshaller[InfraTaskExecution[_]] =
    Marshaller(implicit ec =>
      _.value.runAsync.flatMap {
        case Right(a) => ma.map(me => HttpResponse(entity = me))(a.asInstanceOf[ApiResponse])
        case Left(e) => me.map(me => HttpResponse(status = StatusCodes.InternalServerError, entity = me))(e)
      })

  implicit val assemblerK: AssemblerK[DomainTaskExecution] = new AssemblerK[DomainTaskExecution] {
    override def toDomain[I <: ApiRequest, D <: Domain](from: I)(implicit A: Assembler[I, D]): DomainTaskExecution[D] = EitherT(Task.eval(A.toDomain(from).toEither))
  }

  implicit val disassemblerK: DisassemblerK[DomainTaskExecution, InfraTaskExecution] = new DisassemblerK[DomainTaskExecution, InfraTaskExecution] {
    override def fromDomain[D <: Domain, I <: ApiResponse](from: DomainTaskExecution[D])(implicit D: Disassembler[D, I]): InfraTaskExecution[I] = {
      val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)

      from.bimap(disassembleLeft, D.fromDomain)
    }
  }

  val helloWorldService = HelloWorldService[DomainTaskExecution]()
  val helloWorldRestService = HelloWorldRestService(helloWorldService)
  val helloWorldApi = HelloWorldApi(helloWorldRestService)

  val bindingFuture = Http().bindAndHandle(helloWorldApi.routes, "0.0.0.0", 8080)

  def stop(): Future[Unit] =
    bindingFuture
      .flatMap(_.unbind())
      .flatMap(_ => actorSystem.terminate())
      .map { _ => () }

}

object EitherMain extends App {

  implicit val actorSystem = ActorSystem("hello-world-either")
  implicit val materializer = ActorMaterializer()
  implicit val scheduler: Scheduler = Scheduler(actorSystem.dispatcher)

  type DomainEitherExecution[A] = Either[DomainError, A]
  type InfraEitherExecution[A] = Either[ErrorResponse, A]

  implicit val serialization: Serialization = jackson.Serialization
  implicit val formats: Formats = DefaultFormats + FieldSerializer[DomainError]()

  import de.heikoseeberger.akkahttpjson4s.Json4sSupport.marshaller

  implicit def eitherMarshaller(implicit ma: ToEntityMarshaller[ApiResponse],
                                me: ToEntityMarshaller[ErrorResponse],
                                scheduler: Scheduler): ToResponseMarshaller[InfraEitherExecution[_]] =
    Marshaller(implicit ec => r =>
      Future.successful(r).flatMap {
        case Right(a) => ma.map(me => HttpResponse(entity = me))(a.asInstanceOf[ApiResponse])
        case Left(e) => me.map(me => HttpResponse(status = StatusCodes.InternalServerError, entity = me))(e)
      })

  implicit val assemblerK: AssemblerK[DomainEitherExecution] = new AssemblerK[DomainEitherExecution] {
    override def toDomain[I <: ApiRequest, D <: Domain](from: I)(implicit A: Assembler[I, D]): DomainEitherExecution[D] = A.toDomain(from).toEither
  }

  implicit val disassemblerK: DisassemblerK[DomainEitherExecution, InfraEitherExecution] = new DisassemblerK[DomainEitherExecution, InfraEitherExecution] {
    override def fromDomain[D <: Domain, I <: ApiResponse](from: DomainEitherExecution[D])(implicit D: Disassembler[D, I]): InfraEitherExecution[I] = {
      val disassembleLeft = (domainError: DomainError) => ErrorResponse(domainError.message)

      from.bimap(disassembleLeft, D.fromDomain)
    }
  }

  val helloWorldService = HelloWorldService[DomainEitherExecution]()
  val helloWorldRestService = HelloWorldRestService(helloWorldService)
  val helloWorldApi = HelloWorldApi(helloWorldRestService)

  val bindingFuture = Http().bindAndHandle(helloWorldApi.routes, "0.0.0.0", 8080)

  def stop(): Future[Unit] =
    bindingFuture
      .flatMap(_.unbind())
      .flatMap(_ => actorSystem.terminate())
      .map { _ => () }

}
