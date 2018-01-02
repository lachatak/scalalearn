package org.kaloz.freestyle.domain

import freestyle._

@free trait GreetingAlgebra {
  def greeting(name: String): FS[Greeting]
}