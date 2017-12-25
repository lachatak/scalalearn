package org.kaloz.taglessfinal.domain

import cats.MonadError

case class HelloWorldService[F[_]](implicit M: MonadError[F, DomainError]) {

  def hello(name: String): F[Greeting] = {
    if (name.equals("krs")) {
      M.raiseError(InvalidName(name))
    } else {
      M.pure(Greeting(name))
    }
  }

}