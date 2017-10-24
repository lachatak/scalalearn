package org.kaloz.monocle

import monocle.macros.{GenIso, GenLens}
import monocle.{Iso, Lens, Optional}

object MonocleIsoApp extends App {

  case class Person(name: String, age: Int)

  import monocle.Iso
  import monocle.function.all._

  val personToTuple = Iso[Person, (String, Int)](p => (p.name, p.age)) { case (name, age) => Person(name, age) }

  personToTuple.get(Person("Zoe", 25))
  // res0: (String, Int) = (Zoe,25)

  println(personToTuple.reverseGet(("Zoe", 25)))
  // res1: Person = Person(Zoe,25)

  val stringToList = Iso[String, List[Char]](_.toList)(_.mkString(""))

  println((stringToList composeOptional index(4)).set('R')("Hello"))

  println(stringToList.modify(_.tail)("Hello"))

  val personIso = GenIso.fields[Person].get(Person("John", 42))
  val personIso2 = GenIso.fields[Person]("John", 42)

  println(personIso2)

  val m = Map("one" -> 1, "two" -> 2)

  val root = Iso.id[Map[String, Int]]

  println((root composeLens at("three")).set(Some(3))(m))
}

// Optics
object PhysicalUnitsOptics extends App {

  // business entities
  case class Meter(whole: Int, fraction: Int)

  case class Centimeter(whole: Int)

  val centimeterToMeterIso = Iso[Centimeter, Meter] { cm =>
    Meter(cm.whole / 100, cm.whole % 100)
  } { m =>
    Centimeter(m.whole * 100 + m.fraction)
  }

  import monocle.std.string.stringToInt

  val intCentimeter = Iso[Int, Centimeter](Centimeter.apply)(_.whole)
  val wholeMeterLens = Lens[Meter, Int](_.whole)(newWhole => prevMeter => prevMeter.copy(whole = newWhole))
  val stringToWholeMeter: Optional[String, Int] = stringToInt.
    composeIso(intCentimeter).
    composeIso(centimeterToMeterIso).
    composeLens(wholeMeterLens)

  println(centimeterToMeterIso.modify(m => m.copy(m.whole + 3))(Centimeter(155)))

  println(stringToWholeMeter.modify(_ + 3)("155"))
}

object LensApp extends App {

  case class Address(streetNumber: Int, streetName: String)

  import monocle.macros.GenLens

  val streetNumber = GenLens[Address](_.streetNumber)

  val address = Address(10, "High Street")
  // address: Address = Address(10,High Street)

  streetNumber.get(address)
  // res1: Int = 10

  println(streetNumber.set(5)(address))

  def neighbors(n: Int): List[Int] =
    if (n > 0) List(n - 1, n + 1) else List(n + 1)

  def test(n: Int): Option[Int] =
    if (n > 5 && n < 12) Some(n) else None


  import cats.instances.list._
  import cats.instances.option._

  println(streetNumber.modifyF(neighbors)(address))
  println(streetNumber.modifyF(test)(address))

  val newAddress = streetNumber.set(3)(address)
  println(streetNumber.modifyF(test)(newAddress))


  case class Person(name: String, age: Int, address: Address)

  val john = Person("John", 20, address)

  val personAddress = GenLens[Person](_.address)

  println((personAddress composeLens streetNumber).get(john))

  //Generate Lens as deep as you want
  println(GenLens[Person](_.address.streetName).set("Iffley Road")(john))

  import monocle.macros.Lenses

  @Lenses case class Point(x: Int, y: Int)

  val p = Point(5, 3)

  Point.x.get(p)
  // res14: Int = 5

  Point.y.set(0)(p)
}

object PrismApp extends App {

  sealed trait Json

  case object JNull extends Json

  case class JStr(v: String) extends Json

  case class JNum(v: Double) extends Json

  case class JObj(v: Map[String, Json]) extends Json

  import monocle.Prism

  val jStr = Prism[Json, String] {
    case JStr(v) => Some(v)
    case _ => None
  }(JStr)


  val partialjStr = Prism.partial[Json, String] { case JStr(v) => v }(JStr)

  //apply
  jStr("hello")
  jStr.reverseGet("hello")
  // res1: Json = JStr(hello)

  jStr.getOption(JStr("Hello"))
  // res2: Option[String] = Some(Hello)

  partialjStr.getOption(JNum(3.2))
  // res3: Option[String] = None


  println(partialjStr.modify(_.toUpperCase)(JStr("hello")))
  println(partialjStr.modify(_.toUpperCase)(JNull))
  println(partialjStr.modifyOption(_.toUpperCase)(JNull))


  import monocle.std.double.doubleToInt
  // Prism[Double, Int] defined in Monocle

  val jNum: Prism[Json, Double] = Prism.partial[Json, Double] { case JNum(v) => v }(JNum)

  val jInt: Prism[Json, Int] = jNum composePrism doubleToInt

  println(doubleToInt(5))
  println(doubleToInt.getOption(5.8))
  println(doubleToInt.getOption(5.0))
  println(doubleToInt.reverseGet(5))

  println(jNum.getOption(JNum(5.6)))
  println(jInt.getOption(JNum(5)))


  import monocle.macros.GenPrism

  val rawJNum: Prism[Json, JNum] = GenPrism[Json, JNum]

  println(rawJNum(JNum(4.5)))
  println(rawJNum.getOption(JStr("hello")))


  val jNum2: Prism[Json, Double] = GenPrism[Json, JNum] composeIso GenIso[JNum, Double]
  val jNull: Prism[Json, Unit] = GenPrism[Json, JNull.type] composeIso GenIso.unit[JNull.type]


  println(jNum2(4.5))
  println(jNum2.getOption(JStr("4.5")))


  case class Percent private(value: Int) {
    require(value >= 0)
    require(value <= 100)
  }

  object Percent {
    def fromInt(input: Int): Option[Percent] =
      if (input >= 0 && input <= 100) {
        Some(Percent(input))
      } else {
        None
      }
  }

  val percentLens = GenLens[Percent](_.value)

  import monocle.std.string.stringToInt

  val intToPercentPrism = Prism[Int, Percent](i => Percent.fromInt(i))(_.value)

  val stringToPercent = stringToInt composePrism intToPercentPrism

  println(stringToPercent.getOption("5"))
  println(stringToPercent.reverseGet(Percent(50)))
  println(stringToPercent.modify(p => p.copy(p.value * 2))("20"))

  println((stringToPercent composeLens percentLens).modify(_ + 10)("20"))
}
