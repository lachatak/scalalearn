package org.kaloz.taglessfinal.infrastructure


import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout

import scala.concurrent.duration._

case class HelloWorldApi[F[_], G[_]](helloWorldRestService: HelloWorldRestService[F, G])
                                    (implicit responseMarshaller: ToResponseMarshaller[G[_]]) extends Directives {

  implicit val timeout = Timeout(5 seconds)

  def routes: Route =
    get {
      path("api" / "hello" / Segment) { name =>
        complete {
          helloWorldRestService.hello(name)
        }
      }
    }
}

object HelloWorldApi {

  case class HelloWorldResponse(greeting: String) extends ApiResponse

}
