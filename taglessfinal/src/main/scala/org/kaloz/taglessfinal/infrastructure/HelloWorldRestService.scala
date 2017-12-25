package org.kaloz.taglessfinal.infrastructure

import cats.MonadError
import cats.implicits._
import org.kaloz.taglessfinal.domain.{DomainError, HelloWorldService}
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.HelloWorldResponse

case class HelloWorldRestService[F[_]](helloWorldService: HelloWorldService[F],
                                       helloWorldAssembler: HelloWorldAssembler[F])
                                      (implicit M: MonadError[F, DomainError]) {

  def hello(name: String): F[HelloWorldResponse] =
    for {
      greeting <- helloWorldService.hello(name)
      response <- helloWorldAssembler.disassemble(greeting)
    } yield response
}
