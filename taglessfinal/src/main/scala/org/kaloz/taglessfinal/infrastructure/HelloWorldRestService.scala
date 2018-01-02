package org.kaloz.taglessfinal.infrastructure

import cats.Monad
import cats.implicits._
import org.kaloz.taglessfinal.domain.{Greeting, HelloWorldService, Name}
import org.kaloz.taglessfinal.infrastructure.ApiResponse.ApiResponseSyntax
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.{HelloWorldRequest, HelloWorldResponse}

case class HelloWorldRestService[F[_] : Monad, G[_]](helloWorldService: HelloWorldService[F])
                                                    (implicit A: HelloWorldAssembler[F, G],
                                                     K:AssemblerK[F]) {

  def hello(request: HelloWorldRequest): G[HelloWorldResponse] = {

    val t = K.toDomain[HelloWorldRequest, Name](request)
    val response: F[Greeting] = for {
      name <- request.toDomain[F, Name]
      greeting <- helloWorldService.hello(name)
    } yield greeting

    response.toInfrastructure[G, HelloWorldResponse]
  }
}