package org.kaloz

import scalaz.Scalaz._
import scalaz._

object StateExperiment extends App {

  sealed trait Input

  case object Coin extends Input

  case object Turn extends Input

  case class Machine(locked: Boolean, candies: Int, coins: Int)

  val state = scalaz.StateT.stateMonad[Machine]

  /**
   * The rules of the machine are as follows:
   * 1. Inserting a coin into a locked machine will cause it to unlock if there is any candy left.
   * 2. Turning the knob on an unlocked machine will cause it to dispense candy and become locked.
   * 3. Turning the knob on a locked machine or inserting a coin into an unlocked machine does nothing.
   * 4. A machine that is out of candy ignores all inputs.
   *
   * The method simulateMachine should operate the machine based on the list of inputs
   * and return the number of coins and candies left in the machine at the end.
   * For example, if the input Machine has 10 coins and 5 candies, and a
   * total of 4 candies are successfully bought, the output should be (14, 1).
   */
  def rules(i: Input): State[Machine, (Int, Int)] = for {
    _ <- modify((m: Machine) => (i, m) match {
      case (_, Machine(_, 0, _)) => m // Rule 4
      case (Coin, Machine(false, _, _)) => m // Rule 3b
      case (Turn, Machine(true, _, _)) => m // Rule 3a
      case (Coin, Machine(true, candy, coin)) => Machine(false, candy, coin + 1) // Rule 1
      case (Turn, Machine(false, candy, coin)) => Machine(true, candy - 1, coin) // Rule 2
    })
    m <- get
  } yield (m.coins, m.candies)

  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] = for {
    _ <- state.sequence(inputs.map(rules))
    m <- get[Machine]
  } yield (m.coins, m.candies)

  println(simulateMachine(List(Coin, Turn, Coin, Turn, Coin, Turn, Coin, Turn))(Machine(true, 5, 10)))
}

object FridgeExperimental extends App {


  sealed trait Item {
    def requiredSpace: Int
  }

  case object Beer extends Item {
    val requiredSpace = 1
  }

  case object Milk extends Item {
    val requiredSpace = 1
  }

  case object Potato extends Item {
    val requiredSpace = 2
  }

  case object Food extends Item {
    val requiredSpace = 3
  }

  sealed trait Action

  case class Put(count: Int, item: Item) extends Action

  case class Fetch(count: Int, item: Item) extends Action

  case object Open extends Action

  case object Close extends Action

  case class Fridge(capacity: Int = 10, items: Map[Item, Int] = Map.empty, isOpen: Boolean = false) {
    private val emptySpace: Int = capacity - items.collect { case (item, count) => item.requiredSpace * count}.sum

    def put(count: Int, item: Item): Fridge = count match {
      case x if (x == 0) => this
      case x if (emptySpace >= item.requiredSpace) => copy(items = items |+| Map(item -> 1)).put(count - 1, item)
      case _ => this
    }

    def fetch(count: Int, item: Item): Fridge = items.getOrElse(item, 0) - count match {
      case x if (x <= 0) => copy(items = items - item)
      case _ => copy(items = items + (item -> (items(item) - count)))
    }

    def open = copy(isOpen = true)

    def close = copy(isOpen = false)
  }

  val state = scalaz.StateT.stateMonad[List[Fridge]]

  /**
   * The rules of the fridge are as follows:
   * 1. You can open the fridge if it is closed
   * 2. If the fridge is open you cannot open it even more :)
   * 3. You can close the fridge if it is open
   * 4. If the fridge is close you cannot close it even more :)
   * 5. If the fridge is open you can put item if the capacity allows and you have more space
   * 6. If the fridge is open you can fetch item
   */
  def rules(action: Action): State[List[Fridge], Map[Item, Int]] = for {
    _ <- modify((fridges: List[Fridge]) => (action, fridges.head) match {
      case (Open, f@Fridge(_, _, false)) => f.open :: fridges // Rule 1
      case (Put(count, item), f@Fridge(_, _, true)) => f.put(count, item) :: fridges // Rule 5
      case (Fetch(count, item), f@Fridge(_, _, true)) => f.fetch(count, item) :: fridges // Rule 6
      case (Close, f@Fridge(_, _, true)) => f.close :: fridges // Rule 3
      case (_, _) => fridges // Rule 2, Rule 4
    })
    f <- get[List[Fridge]]
  } yield f.head.items


  def simulateMachine(inputs: List[Action]): State[List[Fridge], Map[Item, Int]] = for {
    _ <- state.sequence(inputs.map(rules))
    f <- get[List[Fridge]]
  } yield f.head.items

  val (s, a) = simulateMachine(List(Open, Put(2, Beer), Put(1, Milk), Put(1, Potato), Close, Open, Fetch(1, Potato), Put(2, Food), Close, Open, Fetch(1, Food), Fetch(1, Beer), Close)) run (List(Fridge()))
  println("Result: " + a)
  println("Log: %n%s".format(s.mkString("\t", "%n\t".format(), "")))
}
