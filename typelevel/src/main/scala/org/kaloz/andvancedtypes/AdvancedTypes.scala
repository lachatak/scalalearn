package org.kaloz.andvancedtypes

package fbound {

  trait Fruit[T <: Fruit[T]] {
    final def compareTo(other: T): Boolean = true // impl doesn't matter in our example
  }

  class Apple extends Fruit[Apple]

  class NewApple extends Apple

  class Orange extends Fruit[Orange]

  trait Account[T <: Account[T]] {
    def addFunds(amount: BigDecimal): T
  }

  class BrokerageAccount(total: BigDecimal) extends Account[BrokerageAccount] {
    def addFunds(amount: BigDecimal) = new BrokerageAccount(total + amount)
  }

  class SavingsAccount(total: BigDecimal) extends Account[SavingsAccount] {
    def addFunds(amount: BigDecimal) = new SavingsAccount(total + amount)
  }

  class MalignantAccount extends Account[SavingsAccount] {
    def addFunds(amount: BigDecimal) = new SavingsAccount(-amount)
  }


  object Account {
    val feePercentage = BigDecimal("0.02")
    val feeThreshold = BigDecimal("10000.00")

    def deposit[T <: Account[T]](amount: BigDecimal, account: T): T = {
      if (amount < feeThreshold) account.addFunds(amount - (amount * feePercentage))
      else account.addFunds(amount)
    }
  }

  object Main extends App {

    val apple = new Apple
    val apple1 = new NewApple
    val orange = new Orange

    apple.compareTo(apple1)

    Account.deposit(BigDecimal(20), new SavingsAccount(BigDecimal(10)))
    Account.deposit(BigDecimal(20), new MalignantAccount)
  }

}
