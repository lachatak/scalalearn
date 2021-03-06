package org.kaloz.taglessfinal.infrastructure

import cats.Monad
import cats.implicits._
import org.kaloz.taglessfinal.domain.{Greeting, HelloWorldService, Name}
import org.kaloz.taglessfinal.infrastructure.ApiResponse.ApiResponseSyntax
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.{HelloWorldRequest, HelloWorldResponse}

trait HelloWorldRestService[G[_]] {
  def hello(request: HelloWorldRequest): G[HelloWorldResponse]
}

case class HelloWorldRestServiceImpl[F[_] : Monad : AssemblerK, G[_]](private val helloWorldService: HelloWorldService[F])
                                                                     (implicit D: DisassemblerK[F, G]) extends HelloWorldRestService[G] {

  def hello(request: HelloWorldRequest): G[HelloWorldResponse] = {
    val response: F[Greeting] = for {
      name <- request.toDomain[Name]
      greeting <- helloWorldService.hello(name)
    } yield greeting

    response.toInfrastructure[HelloWorldResponse]
  }
}