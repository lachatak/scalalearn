package org.kaloz

import org.apache.spark.SparkContext

import scalaz._
import Scalaz._

object MonadTransformers extends App {

  type Error[+A] = \/[String, A]
  type Result[A] = OptionT[Error, A]

  val result: Result[Int] = 42.point[Result]

//  val t : Error[Option[Int]] = "Error message".left
//
//  println(t)
//  OptionT(t)
  val failure: MonadTransformers.Result[Int] = OptionT("Error message".left : Error[Option[Int]])

  val transformed =
    for {
      value <- result
    } yield value



  result.map(x => x + 2)
  // scalaz.OptionT[Error,Int] = OptionT(\/-(Some(44)))
  val r2 = result.flatMap(_ => "Yeah!".point[Result])
  // scalaz.OptionT[Error,java.lang.String] = OptionT(\/-(Some(Yeah!)))

  println(result)
  println(r2.run)
  println(transformed)
  println(failure)


  type IntReader[A] = Reader[Int, A]
  type IntConfig[A] = OptionT[IntReader, A]

  def config(i:Int):IntConfig[Int] = OptionT(Reader( t => (t + i).some  ):IntReader[Option[Int]] )
  def config2(i:Int):IntConfig[Int] = OptionT(Reader( t => (t * i).some  ):IntReader[Option[Int]] )


  val t = for{
    c <- config(3)
    r <- config2(c)
  } yield r

  println(config(1).run(2))
  println(config2(1).run(2))
  println(t.run(2))


}
