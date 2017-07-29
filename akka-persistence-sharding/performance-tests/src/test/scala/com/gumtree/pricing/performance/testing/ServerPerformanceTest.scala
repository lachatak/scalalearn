package com.gumtree.pricing.performance.testing

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.netty.handler.codec.http.HttpResponseStatus

class ServerPerformanceTest extends Simulation {

  // Default configuration
  val httpProtocol = http.baseURL("http://127.0.0.1").disableWarmUp

  val id = new AtomicInteger(2000)

  val feeder = Iterator.continually(Map("id" -> id.getAndIncrement()))

  // Request URL details
  val token = "/token/${id}"
  val execute = "/execute/${id}"

  val serverScenario = scenario("Server Test")
    .feed(feeder)
    .exec(http("token")
      .get(token)
      .check(status.is(HttpResponseStatus.OK.code()), status.saveAs("status")))
    .doIf(session => session("status").as[Int] == HttpResponseStatus.OK.code()) {
      exec(http("execute")
        .get(execute)
        .check(status.is(HttpResponseStatus.OK.code())))
    }

  val num = 100

  // Simulation set-up
  setUp(serverScenario.inject(rampUsers(num * 4) over (num)))
    .protocols(httpProtocol)
    .assertions(
      global.failedRequests.percent.lt(1),
      global.responseTime.mean.lt(1000))
}
