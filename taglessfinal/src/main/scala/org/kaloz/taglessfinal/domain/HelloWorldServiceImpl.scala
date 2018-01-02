package org.kaloz.taglessfinal.domain

import cats.MonadError

trait HelloWorldService[F[_]] {
  def hello(name: Name): F[Greeting]
}

case class HelloWorldServiceImpl[F[_]]()(implicit M: MonadError[F, DomainError]) extends HelloWorldService[F] {

  def hello(name: Name): F[Greeting] =
    if (name.name.equals("krs")) {
      M.raiseError(NotSupportedName(name.name))
    } else {
      M.pure(Greeting(name.name))
    }

}