package org.kaloz.taglessfinal.domain

import cats.implicits._
import org.kaloz.taglessfinal.TestDomainType
import org.scalatest.{EitherValues, Matchers, WordSpecLike}

class HelloWorldServiceImplSpec extends WordSpecLike with Matchers with EitherValues {

  "HelloWorldService.hello" should {

    "return Greeting when the name is supported" in new scope {
      helloWorldService.hello(Name.unsafe(supportedName)).right.value shouldBe Greeting(supportedName)
    }

    "return NotSupportedName when the name is not supported" in new scope {
      helloWorldService.hello(Name.unsafe(notSupportedName)).left.value shouldBe NotSupportedName(notSupportedName)
    }
  }

  private trait scope {
    val supportedName = "test"
    val notSupportedName = "krs"
    val helloWorldService = HelloWorldServiceImpl[TestDomainType]()
  }

}
