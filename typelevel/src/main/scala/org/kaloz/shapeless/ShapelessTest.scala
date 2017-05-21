package org.kaloz.shapeless

object ShapelessTest extends App {

  import shapeless._
  import shapeless.ops.hlist

  //  case class A(
  //                a: String = "a",
  //                a1: String = "a",
  //                a2: String = "a",
  //                a3: String = "a",
  //                a4: String = "a",
  //                a5: String = "a",
  //                a6: String = "a",
  //                a7: String = "a",
  //                a8: String = "a",
  //                a9: String = "a",
  //                a10: String = "a",
  //                a11: String = "a",
  //                a12: String = "a",
  //                a13: String = "a"
  //              )
  //
  //  case class B(
  //                a2: String = "a",
  //                a5: String = "a",
  //                a6: String = "a",
  //                a9: String = "a",
  //                a10: String = "a",
  //                a11: String = "a"
  //              )

  case class A(
                a: String = "a",
                a1: String = "a",
                a2: String = "a",
                a3: String = "a"
              )

  case class B(
                a: String = "a",
                a2: String = "a"
              )

  val aGen = Generic[A]
  val bGen = Generic[B]

  val aRepr:HList = aGen.to(A())
  val BRepr = bGen.to(B())

  println(aRepr)

  def myMethod[A, B, ARepr <: HList, BRepr <: HList](source:A, target:Class[B])(implicit aGen: Generic.Aux[A, ARepr],
                                                                                bGen: Generic.Aux[B, BRepr],
                                                                                inter: hlist.Intersection.Aux[ARepr, BRepr, BRepr]): B = {

    bGen.from(inter(aGen.to(source)))
  }

  println(myMethod(A(), classOf[B]))

  val t = "krs" :: true :: HNil
  type Repr = Int :: String :: HNil
  println(t.intersect[Repr])

}
