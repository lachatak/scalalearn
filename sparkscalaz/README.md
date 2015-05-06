## Spark with Scalaz ##
Recently I have had a little spare time to play around with [Spark](https://spark.apache.org/). It promisses not less than a 100x faster computation than Hadoop MapReduce. I used to play with Spark, Akka, Camel setup within my [camel-spark](https://github.com/lachatak/camel-spark) project but now I wanted to use it together with Scalaz. 

I am runnig a **Dev challenges** program with [Mate](http://tindaloscode.blogspot.co.uk/) at Gamesys to form a developer community and promote continous learning. The first challenge was about a big data aggregator application. The description can be found [here](https://github.com/lachatak/dev-challenges/blob/master/bigdataaggregator/CHALLENGE.md).
The spark solution in this repository is a bit simplified version of my original solution and provides a good context to practice Scalaz and Scala concepts like **Reader** or **State** monad.

There are two solutions here.
- Spark with Scalaz' **Reader** monad. **Reader** monad is used as a kind of dependency injection solution. I believe this is a typical problem where you could harness its features.
- Spark with Scalaz' **ReaderWriterState** monad. I use this monad for multiple purposes. **Reader** monad provides a frame for dependency injection, **Writer** helps to eliminate side effects of logging messages meanwhile **State** stores temporary results during the prcession.

I won't explain the first solution as it is part of the second one. So let's turn our intention to the **ReaderWriterState** monad and it goodiness.

### The Problem ###
You would like to use Spark for a distributed computation. I simple and working solution for the requirements:
```scala
  def process(targetCurrency: String) = {
    val sc = new SparkContext(new SparkConf().setAppName("challenge").setMaster("local[8]"))

    try {
      val results = parse(sc, targetCurrency)
      println(results.toList)

    } finally {
      sc.stop
    }
  }

  def parse(sc: SparkContext, targetCurrency: String) = {
    val rates = sc.textFile("exchangerates.csv")
      .map(_.split(","))
      .filter { case Array(from, to, rate) => to == targetCurrency }
      .map { case Array(from, to, rate) => (from, rate.toDouble) }
      .collect()
      .toMap

    def exchange(currency: String, amount: String) = if (currency == targetCurrency) amount.toDouble else amount.toDouble * rates(currency)

    sc.textFile("transactions.csv")
      .map(_.split(","))
      .map { case Array(partner, currency, amount) => (partner, exchange(currency, amount)) }
      .reduceByKey(_ + _)
      .collect()
  }
  ```
  It has couple of problems. I know it is speculative but what if?
  - you need just the excahange rates?
  - you need the exchange rates for other calculations as well?
  - you need the spark context itself to do some other calculations before you close it?
  - you need the result of aggregated data to do some extra calculations on it?
  This solution is just not provides freedom and flexibility. I know you have to focus on your current task and problem. I know [YAGNI](http://en.wikipedia.org/wiki/You_aren%27t_gonna_need_it) as well but still. Could we provide better solution? I suppose yes.

### The solution ###
I called **ReaderWriterState** monad from Scalaz to help me out.

#### Reader monad part ####
We would like to have a context and optionally we would like to reuse this context for multiple calculations. There is no point in opening and closing Spark context every time we need some disributed computation especially if those subsequent computations happen on the same context. We should be able chain them for the maximum efficiency. **Reader** monad gives us this freedom. You can easily define reusable building blockes for the computation which depends on some context which is here the Spark context itself. With this approach we could chain Spark computations using the same Spark context. All we have to do is defining individual building blocks, individual computations. Chaining the blocks just prvide the definition of the computation itself without really running it. It will happen when we finally call the **run** method of the **Reader** with a real Spark context. It will be inject to the computation itself and the defined building block will be executed.

#### Writer monad part ####
I wanted to have audit log about the computation. **Writer** monad collects all your log messages and makes it availble after the process has finished. You can do what ever you want with your logs later.

#### State monad part ####
I wanted to reuse intermediate results for later calculations. We could achieve this with a simple Reader monad as well but I believe **State** monad gives you more flexibility and options. A soon as you need some return value from one building block and you also would like to modify and reuse a previous calculation again you will find Reader monad more restrictive.
So **State** provides means to mantain and propagat intermediate results to the next building block meanwhile **Reader** gives you always a predefined context what you can reuse again and again without being able to modify it. Effectively provides dependency injection.

#### ReaderWriterState monad ####
**ReaderWriterState** monad gives us all the freedom we need to be able to combine computation working blocks together.

How it looks like in code?
I defined the following type:
```scala
  type Work[S, A] = ReaderWriterState[SparkContext, List[String], S, A]
```
Every working block should produce a Work[S, A] type response. What it says is the following:
- Every computation depends on a Spark context
- Every computation should contribute to the audit log with a list of messages
- Every computation should maintain a state which has a type S
- Every computation should provide some results
Effectively you have to provide the following for every building blocks:
```scala
ReaderWriterState((SparkContext, S) =>(List[String], A, S))
```
Let's see how it looks like in the practice:
```scala
  def rates[S](exchangerates: String, currency: String): Work[S, Map[String, BigDecimal]] =
    ReaderWriterState((sc, state) => {
      val rates = sc.textFile(exchangerates)
        .map(_.split(",").map(_.trim.toUpperCase))
        .collect { case Array(from, to, amount) if (currency == to) => (from -> BigDecimal(amount))}
        .collectAsMap()
        .toMap
      (s"${rates.size} exchangerate(s) has been loaded.." :: Nil, rates, state)
    })
```
We use the context here but we don't need the state. It just simple loads the exchange rates. I extracted the effective coputation part before the tiple as I wanted to log some results from the computation.
```scala
  def summaryByCurrency(transactions: String, currency: String, rates: Map[String, BigDecimal]): Work[Map[String, BigDecimal], Map[String, BigDecimal]] =
    ReaderWriterState((sc, state) => {
      val result = sc.textFile(transactions)
        .map(_.split(",").map(_.trim.toUpperCase))
        .map { case Array(partner, to, amount) => (partner, rates.getOrElse(to, BigDecimal(0)) * BigDecimal(amount))}
        .reduceByKey(_ + _)
        .collectAsMap()
        .toMap
      (s"${result.size} partner(s) has been loaded.." :: Nil, result, result)
    })
```
The same concept but here we save the computation result as a state meanwhile returning it as well. With this little twist every subsequent block will be able to use the incoming state part as well.
```scala
  def summaryByPartner(partner: String): Work[Map[String, BigDecimal], BigDecimal] =
    ReaderWriterState((sc, state) => {
      val result = state(partner.toUpperCase)
      (s"Result for $partner has been fetched -> $result" :: Nil, result, state)
    })
```
Here I am just using the state to fetch an amount belongs to a specific partner.
And finally the logging itself:
```scala
  def log(message: String): Work[Map[String, BigDecimal], Unit] =
    ReaderWriterState((sc, state) => (message :: Nil, (), state))
```
It simply adds the incoming message the the audit log without touching the state and providing any results.

Now we have the building blockes. Lets put those block together. This is the way how you could form bigger functions from dump elements:
```scala
  def loadSummaryByCurrency(exchangerates: String = "exchangerates.csv", transactions: String = "transactions.csv", currency: String = "GBP", partner: String = "Unlimited ltd."): Work[Map[String, BigDecimal], BigDecimal] =
    for {
      _ <- log(s"Loading exchangerates from $exchangerates file!")
      r <- rates(exchangerates, currency)
      _ <- log(s"Calculating transaction summary from $transactions file!")
      _ <- summaryByCurrency(transactions, currency, r)
      _ <- log(s"Process done!")
      s <- summaryByPartner(partner)
    } yield s
```
Now we have a fully defined coputation. Dont forget it is just a definition nothing else. It gives back a unit of Work[S, A]. It means that the state has type Map[String, BigDecimal] meanwhile the result of the entire block will be a BigDecimal. Nice and easy.
All we have left is to run it. First thing we need is a Spark context. The following method only accept a type Work. You cannot run anything with this which is not a type Work. Meanwhile the Work requires the context. Win-Win. So let's provide the context which only could run a Work:
```scala
  def runWithSpark[S, A](work: Work[S, A])(implicit S: Monoid[S]): (List[String], A, S) = {

    val sparkConf: SparkConf = new SparkConf()
      .setAppName("readerWriterState")
      .setMaster("local[8]")
      .setJars(SparkContext.jarOfClass(this.getClass).toSeq)

    val sc = new SparkContext(sparkConf)

    try {
      work.run(sc, S.zero)
    } finally {
      sc.stop
    }
  }
```
There is a bit magic with the Monoid implicit which is required to be able to change the type of the state based on what type of work we have. As for us it will be a Map[String, BigDecimal] the implicit gives us an empty map to be able to kick start the computation. The Spark context is just defined inside the method and injected to the work. After this point our computation definition really starts to work and produces a result. As you can see it will be a triple. 
- List of messages were generated during the process
- The final computation result which is a BigDecimal now
- The current state which is a Map with all the partners
The rest is up to you how you use the results.

### Conclusions ###
I believe that the **ReaderWriterState** monad is a really good tool worth playing around with.
I hope I managed to show you some interesting ideas about it and also persuaded you that sometime it is beneficial to look a bit farther than your actual task. Though bear in mind ***YAGNI***!! 






  
