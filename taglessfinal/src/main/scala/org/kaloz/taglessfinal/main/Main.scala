package org.kaloz.taglessfinal.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import cats.data.EitherT
import monix.cats.monixToCatsMonad
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.{DefaultFormats, FieldSerializer, Formats, Serialization, jackson}
import org.kaloz.taglessfinal.domain.{Domain, DomainError, HelloWorldService}
import org.kaloz.taglessfinal.infrastructure.{ApiResponse, ErrorResponse, HelloWorldApi, HelloWorldAssemblerInterpreter, HelloWorldRestService}

import scala.concurrent.Future

object Main extends App {

  implicit val actorSystem = ActorSystem("hello-world")
  implicit val materializer = ActorMaterializer()
  implicit val scheduler: Scheduler = Scheduler(actorSystem.dispatcher)

  type DomainExecution[A] = EitherT[Task, DomainError, A]
  type InfraExecution[A] = EitherT[Task, ErrorResponse, A]

  implicit val serialization: Serialization = jackson.Serialization
  implicit val formats: Formats = DefaultFormats + FieldSerializer[DomainError]()

  import de.heikoseeberger.akkahttpjson4s.Json4sSupport.marshaller

  implicit def eitherTMarshaller(implicit ma: ToEntityMarshaller[ApiResponse],
                                 me: ToEntityMarshaller[ErrorResponse],
                                 scheduler: Scheduler): ToResponseMarshaller[InfraExecution[_]] =
    Marshaller(implicit ec =>
      _.value.runAsync.flatMap {
        case Right(a) => ma.map(me => HttpResponse(entity = me))(a.asInstanceOf[ApiResponse])
        case Left(e) => me.map(me => HttpResponse(status = StatusCodes.InternalServerError, entity = me))(e)
      })

  val helloWorldService = HelloWorldService[DomainExecution]()
  val helloWorldAssembler = HelloWorldAssemblerInterpreter()
  val helloWorldRestService = HelloWorldRestService(helloWorldService, helloWorldAssembler)
  val helloWorldApi = HelloWorldApi(helloWorldRestService)

  val bindingFuture = Http().bindAndHandle(helloWorldApi.routes, "0.0.0.0", 8080)

  def stop(): Future[Unit] =
    bindingFuture
      .flatMap(_.unbind())
      .flatMap(_ => actorSystem.terminate())
      .map { _ => () }

}
