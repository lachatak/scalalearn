package org.kaloz.taglessfinal.domain

trait HelloWorldRepository[F[_]] {
  def findBy(name: Name): F[Option[Person]]
}
