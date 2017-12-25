package org.kaloz.taglessfinal.domain

import cats.MonadError

case class HelloWorldService[F[_]](implicit M: MonadError[F, Throwable]) {

  def hello(name: String): F[Greeting] = {
    if (name.equals("krs")) {
      M.raiseError(new RuntimeException("Invalid name!"))
    } else {
      M.pure(Greeting(name))
    }
  }

}