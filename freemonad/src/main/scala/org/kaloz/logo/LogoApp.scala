package org.kaloz.logo

import cats.implicits._
import cats.free.Free
import cats.{Id, Monad}
import io.aecor.liberator.{FreeAlgebra, ProductKK}

object LogoApp extends App {

  def program[F[_] : Monad : Instruction : PencilInstruction](start: Position): F[Position] =
    for {
      p1 <- Instruction[F].forward(start, 10)
      p2 <- Instruction[F].right(p1, Degree(90))
      _ <- PencilInstruction[F].pencilUp(p2)
      p3 <- Instruction[F].forward(p2, 10)
      _ <- PencilInstruction[F].pencilDown(p3)
      p4 <- Instruction[F].backward(p3, 20)
      _ <- Instruction[F].showPosition(p4)
    } yield p4

  val startPosition = Position(0.0, 0.0, Degree(0))

  val freeAlgebra = FreeAlgebra[ProductKK[Instruction, PencilInstruction, ?[_]]]

  val freeProgram = program[Free[freeAlgebra.Out, ?]](startPosition)

  //  import monix.cats._
  //  import monix.eval.Task
  //  import monix.execution.Scheduler.Implicits.global
  //  val out = freeProgram.foldMap(freeAlgebra(ProductKK(LogoInstruction[Task], ConsolePencilInstruction[Task])))
  //  out.runAsync

  //  import cats.Eval
  //  val out = freeProgram.foldMap(freeAlgebra(ProductKK(LogoInstruction[Eval], ConsolePencilInstruction[Eval])))
  //  out.value

  val out = freeProgram.foldMap(freeAlgebra(ProductKK(LogoInstruction[Id], ConsolePencilInstruction[Id])))

}

