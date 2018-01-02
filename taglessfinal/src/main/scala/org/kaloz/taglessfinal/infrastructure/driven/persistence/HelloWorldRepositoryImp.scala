package org.kaloz.taglessfinal.infrastructure.driven.persistence

import cats.Monad
import org.kaloz.taglessfinal.domain.{HelloWorldRepository, Name, Person}
import org.kaloz.taglessfinal.infrastructure.driven.{AssemblerK, DisassemblerK}

case class HelloWorldRepositoryImp[F[_] : Monad : AssemblerK, P[_]]()(implicit D: DisassemblerK[F, P]) extends HelloWorldRepository[F]{
  override def findBy(name: Name): F[Option[Person]] = {
    for{

    }
  }
}
