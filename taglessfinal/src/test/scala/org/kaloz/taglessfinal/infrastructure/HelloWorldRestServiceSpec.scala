package org.kaloz.taglessfinal.infrastructure

import cats.implicits._
import org.kaloz.taglessfinal.TestDomainType
import org.kaloz.taglessfinal.domain.{Greeting, HelloWorldService, Name, NotSupportedName}
import org.kaloz.taglessfinal.infrastructure.HelloWorldApi.{HelloWorldRequest, HelloWorldResponse}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{EitherValues, Matchers, WordSpecLike}

class HelloWorldRestServiceSpec extends WordSpecLike with Matchers with EitherValues with MockFactory {

  "HelloWorldRestService.hello" should {

    "return HelloWorldResponse when name is supported" in new scope {
      (helloWorldServiceMock.hello _).expects(Name.unsafe(supportedName)).returns(Greeting(supportedName).asRight)

      helloWorldRestService.hello(HelloWorldRequest(supportedName)).right.value shouldBe HelloWorldResponse(supportedName)
    }

    "return ErrorResponse when name is not supported" in new scope {
      (helloWorldServiceMock.hello _).expects(Name.unsafe(notSupportedName)).returns(NotSupportedName(notSupportedName).asLeft)

      helloWorldRestService.hello(HelloWorldRequest(notSupportedName)).left.value should matchPattern { case ErrorResponse(_) => }
    }

  }

  private trait scope {
    val supportedName = "supported"
    val notSupportedName = "notSupported"
    val helloWorldServiceMock = mock[HelloWorldService[TestDomainType]]
    val helloWorldRestService = HelloWorldRestServiceImpl(helloWorldServiceMock)
  }

}
