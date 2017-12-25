package org.kaloz.taglessfinal.domain

import cats.MonadError

case class HelloWorldService[F[_]](implicit M: MonadError[F, DomainError]) {

  def hello(name: Name): F[Greeting] = {
    if (name.name.equals("krs")) {
      M.raiseError(InvalidName(name.name))
    } else {
      M.pure(Greeting(name.name))
    }
  }

}