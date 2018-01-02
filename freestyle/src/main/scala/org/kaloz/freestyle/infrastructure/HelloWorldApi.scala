package org.kaloz.freestyle.infrastructure


import akka.http.scaladsl.model.Uri.Path.Segment
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import monix.execution.Scheduler
import akka.http.scaladsl.server.{Directives, Route}

import scala.concurrent.duration._

class GreetingApi(greetingRestService: GreetingRestService)(implicit scheduler: Scheduler) extends Directives {

  implicit val timeout = Timeout(5 seconds)

  def routes: Route =
    get {
      path("api" / "hello" / Segment) { name =>
        complete {
          greetingRestService.greeting(name).runAsync
        }
      }
    }
}

object GreetingApi {

  def apply(greetingRestService: GreetingRestService)(implicit scheduler: Scheduler): GreetingApi =
    new GreetingApi(greetingRestService)(scheduler)

}

