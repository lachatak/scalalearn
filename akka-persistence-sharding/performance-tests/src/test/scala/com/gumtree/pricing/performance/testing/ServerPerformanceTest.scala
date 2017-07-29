package com.gumtree.pricing.performance.testing

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.netty.handler.codec.http.HttpResponseStatus
//import scala.concurrent.duration._

import scala.util.Random

class ServerPerformanceTest extends Simulation {

  val ips = List("8080", "8081", "8082", "8083")
//  val ips = List("8080", "8083")

  // Default configuration
  val httpProtocol = http.baseURL("http://127.0.0.1")

  val id = new AtomicInteger(1000)

  val feeder = Iterator.continually(Map("id" -> id.getAndIncrement()))
  val portfeeder = Iterator.continually(Map("port" -> Random.shuffle(ips).head))

  // Request URL details
  val token = ":${port}/token/${id}"
  val execute = ":${port}/execute/${id}"

  val serverScenario = scenario("Server Test")
    .feed(feeder)
    .feed(portfeeder)
    .exec(http("token")
      .get(token)
      .check(status.is(HttpResponseStatus.OK.code())))
    //    .pause(2 second)
    .feed(portfeeder)
    .exec(http("exec")
      .get(execute)
      .check(status.is(HttpResponseStatus.OK.code())))

  val num = 500

  // Simulation set-up
  setUp(serverScenario.inject(rampUsers(num*2) over (num)))
    .protocols(httpProtocol)
    .assertions(
      global.failedRequests.percent.lt(1),
      global.responseTime.mean.lt(1000))
}
