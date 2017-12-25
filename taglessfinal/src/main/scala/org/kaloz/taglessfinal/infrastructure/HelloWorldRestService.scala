package org.kaloz.taglessfinal.infrastructure

import cats.Monad
import cats.implicits._
import org.kaloz.taglessfinal.domain.{Greeting, HelloWorldService}
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.HelloWorldResponse

case class HelloWorldRestService[F[_] : Monad, G[_]](helloWorldService: HelloWorldService[F],
                                                     helloWorldAssembler: HelloWorldAssembler[F, G]) {

  def hello(name: String): G[HelloWorldResponse] = {
    val result: F[Greeting] = for {
      name <- helloWorldAssembler.assemble(name)
      greeting <- helloWorldService.hello(name)
    } yield greeting

    helloWorldAssembler.disassemble(result)
  }
}
