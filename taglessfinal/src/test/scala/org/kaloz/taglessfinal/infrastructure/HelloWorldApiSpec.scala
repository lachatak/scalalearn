package org.kaloz.taglessfinal.infrastructure

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.kaloz.taglessfinal._
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.{HelloWorldRequest, HelloWorldResponse}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}

class HelloWorldApiSpec extends WordSpecLike with Matchers with MockFactory with ScalatestRouteTest with Json4sSupport {

  "HelloWorldApi.hello" should {

    "return HelloWorldResponse when request was successful" in new scope {
      val response = HelloWorldResponse(validName)

      (helloWorldRestServiceStub.hello _)
        .expects(HelloWorldRequest(validName))
        .returning(response.asRight)

      Get(s"/api/hello/$validName") ~> helloWorldApi.routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[HelloWorldResponse] should be(response)
      }
    }

    "return ErrorResponse when request failed" in new scope {
      val response = ErrorResponse(invalidName)

      (helloWorldRestServiceStub.hello _)
        .expects(HelloWorldRequest(invalidName))
        .returning(response.asLeft)

      Get(s"/api/hello/$invalidName") ~> helloWorldApi.routes ~> check {
        status shouldBe StatusCodes.InternalServerError
        responseAs[ErrorResponse] should be(response)
      }
    }
  }

  private trait scope {
    val validName = "validName"
    val invalidName = "invalidName"

    val helloWorldRestServiceStub = mock[HelloWorldRestService[TestInfraType]]
    val helloWorldApi = HelloWorldApi(helloWorldRestServiceStub)
  }

}
