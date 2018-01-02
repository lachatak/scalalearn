package org.kaloz.taglessfinal.infrastructure


import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import org.kaloz.taglessfinal.domain.{Greeting, Name}
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.HelloWorldRequest

import scala.concurrent.duration._

case class HelloWorldApi[G[_]](helloWorldRestService: HelloWorldRestService[G])
                              (implicit responseMarshaller: ToResponseMarshaller[G[_]]) extends Directives {

  implicit val timeout = Timeout(5 seconds)

  def routes: Route =
    get {
      path("api" / "hello" / Segment) { name =>
        complete {
          helloWorldRestService.hello(HelloWorldRequest(name))
        }
      }
    }
}

object HelloWorldApi {

  case class HelloWorldRequest(name: String) extends ApiRequest

  object HelloWorldRequest {
    implicit val toName: Assembler[HelloWorldRequest, Name] = (request: HelloWorldRequest) => Name(request.name)
  }

  case class HelloWorldResponse(greeting: String) extends ApiResponse

  object HelloWorldResponse {
    implicit val toHelloWorldResponse: Disassembler[Greeting, HelloWorldResponse] = (domain: Greeting) => HelloWorldResponse(domain.message)
  }

}

