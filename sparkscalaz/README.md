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
    val sc = new SparkContext(new SparkConf().setAppName("challenge1").setMaster("local[2]"))

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

### ReaderWriterState monad ###
**ReaderWriterState** monad gives us all the freedom we need to be able to combine working blocks together.

### Reader monad part ###
We would like to have a context and optionally we would like to reuse this context for multiple calculations. There is no point in opening and closing Spark context every time we need some disributed computation especially if those subsequent computations happen on the same context. We should be able chain them for the maximum efficiency. **Reader** monad gives us this freedom. You can easily define reusable building blockes for the computation which depends on some context which is here the Spark context itself. With this approach we could chain Spark computations using the same Spark context. All we have to do is defining individual building blocks, individual computations. Chaining the blocks just prvide the definition of the computation itself without really running it. It will happen when we finally call the **run** method of the **Reader** with a real Spark context. It will be inject to the computation itself and the defined building block will be executed.

### Writer monad part ###
I wanted to have audit log about the computation. **Writer** monad collects all your log messages and makes it availble after the process has finished. You can do what ever you want with your logs later.

### State monad part ###
I wanted to reuse intermediate results for later calculations. We could achieve this with a simple Reader monad as well but I believe **State** monad gives you more flexibility and options. A soon as you need some return value from one building block and you also would like to modify and reuse a previous calculation again you will find Reader monad more restrictive.
So **State** provides means to mantain and propagat intermediate results to the next building block meanwhile **Reader** gives you always a predefined context what you can reuse again and again without being able to modify it. Effectively provides dependency injection.

How it looks like in code?

```scala
```


  
