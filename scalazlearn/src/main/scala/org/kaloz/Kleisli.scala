package org.kaloz

import scala.util._
import scalaz.Kleisli._
import scalaz.Scalaz._

object KleisliUsage extends App {

  // just some trivial data structure ,
  // Continents contain countries. Countries contain cities.
  case class Continent(name: String, countries: List[Country] = List.empty)

  case class Country(name: String, cities: List[City] = List.empty)

  case class City(name: String, isCapital: Boolean = false, inhabitants: Int = 20)

  val data: List[Continent] = List(
    Continent("Europe"),
    Continent("America",
      List(
        Country("USA",
          List(
            City("Washington"), City("New York"))))),
    Continent("Asia",
      List(
        Country("India",
          List(City("New Dehli"), City("Calcutta"))))))

  def continents(name: String): List[Continent] =
    data.filter(k => k.name.contains(name))

  def countries(continent: Continent): List[Country] = continent.countries

  def cities(country: Country): List[City] = country.cities

  def save(cities: List[City]): Try[Unit] =
    Try {
      cities.foreach(c => println("Saving " + c.name))
    }

  def inhabitants(c: City): Int = c.inhabitants

  // allCities and allCities are examples of using the variations of the
  // andThen operator, either starting with a kleisli arrow and following with
  // functions of the form A => M[B] or following with adequate kleisli arrows.
  // The aliases are: >==> and andThenK
  //                  >=>  and andThen
  // the same applies to function composition with
  // <==<, <=<, composeK and compose
  val allCities = kleisli(continents) >==> countries >==> cities
  val allCities2 = kleisli(continents) >=> kleisli(countries) >=> kleisli(cities)

  (allCities("America")).map(println)

  // =<< takes a monadical structure compatible with the kleislifunction
  // as its parameter and flatmaps the function over this parameter.
  (allCities =<< List("Amer", "Asi")).map(println)

  // with map we can map a function B => C over a kleisli function of the
  // structure A => M[B]
  val cityInhabitants = allCities map inhabitants
  (cityInhabitants =<< List("Amer", "Asi")).map(println)


  // with mapK you can map a kleisli function into
  // another monadic structure, e.g. provide a function
  // M[A] => N[B]
  // Note : the example is not particularily useful here.
  val getandSave = (allCities mapK save)
  getandSave("America").map(println)

  // local can be used to prepend a kleisli function of
  // the form A => M[B] with a function of the form
  // AA => A, resulting in a kleisli function of the form
  // AA => M[B]
  def index(i: Int) = data(i).name

  val allCitiesByIndex = allCities local index
  allCitiesByIndex(1).map(println)


  // A=>M[B]
  def toInt(x: String): Option[Int] = Some(x.toInt)

  // B=>M[C]
  def add(x: Int): Option[Int] = Some(x + 2)

  // C=>M[D]
  def double(x: Int): Option[Double] = Some(x * 2)

  //M[D]=>N[E]
  def digits(d: Option[Double]): List[Char] = d.fold(List.empty[Char])(_.toString.toList)

  def duplicate(c: Char): List[Char] = c :: c :: Nil

  def oldSchool(i: String) =
    for {
      x <- toInt(i)
      y <- add(x)
      z <- double(y)}
    yield z

  println(oldSchool("5"))

  // The aliases are: >==> and andThenK
  // create a kleisli from not kleisli function and than andThanK
  //Kleisli[Option, String, Double]
  val funky = kleisli(toInt) >==> add >==> double
  println(funky("5"))

  //create a kleisli from not kleisli function and than feed it with a same type of monad with =<<
  //Kleisli[Option, String, Double]
  println(funky =<< Some("5"))

  //create a kleisli from not kleisli function and compose with <==< or composeK
  //Kleisli[Option, String, Double]
  val funky2 = funky <==< ((str: String) => Some(str + str))
  println(funky2("5"))

  val funky3 = funky2 mapK digits
  println(funky3("5"))

  val funky4 = funky3 >==> duplicate
  println(funky4("5"))

  println(toInt("5") >>= add >>= double)
}