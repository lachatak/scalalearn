package org.kaloz.logo

import cats.Applicative
import io.aecor.liberator.macros.free

case class Position(x: Double, y: Double, heading: Degree)

  case class Degree(private val d: Int) {
    val value = d % 360
  }

  @free
  trait Instruction[F[_]] {
    def forward(pos: Position, l: Int): F[Position]

    def backward(pos: Position, l: Int): F[Position]

    def left(pos: Position, d: Degree): F[Position]

    def right(pos: Position, d: Degree): F[Position]

    def showPosition(pos: Position): F[Unit]
  }

  class LogoInstruction[F[_] : Applicative] extends Instruction[F] {
    override def forward(pos: Position, l: Int): F[Position] = Applicative[F].pure(Computations.forward(pos, l))

    override def backward(pos: Position, l: Int): F[Position] = Applicative[F].pure(Computations.backward(pos, l))

    override def left(pos: Position, d: Degree): F[Position] = Applicative[F].pure(Computations.left(pos, d))

    override def right(pos: Position, d: Degree): F[Position] = Applicative[F].pure(Computations.right(pos, d))

    override def showPosition(pos: Position): F[Unit] = Applicative[F].pure(println(s"showing position $pos"))
  }

  object LogoInstruction {
    def apply[F[_] : Applicative]: Instruction[F] = new LogoInstruction[F]
  }

  @free
  trait PencilInstruction[F[_]] {
    def pencilUp(pos: Position): F[Unit]

    def pencilDown(pos: Position): F[Unit]
  }

  class ConsolePencilInstruction[F[_] : Applicative] extends PencilInstruction[F] {
    override def pencilUp(pos: Position): F[Unit] = Applicative[F].pure(println(s"start drawing at $pos"))

    override def pencilDown(pos: Position): F[Unit] = Applicative[F].pure(println(s"stop drawing at $pos"))
  }

  object ConsolePencilInstruction {
    def apply[F[_] : Applicative]: PencilInstruction[F] = new ConsolePencilInstruction[F]
  }
