## Zip Decimals ##
I have encountered recently the following zip problem. I provided a simple solution for this. To be honest not the solution itself but the fact that how smooth you can integrate scala and java together in the very same project. Here just for testing purpose but it still shows its efficiency. The other reason for this post is the fact that I have just rediscovered how good is the property based testing :)

### Rules ###
Given 2 decimals between 0 and 10.000.000 call them A and B. Zip the 2 decimals to C applying the following rules:
- C's first most significant digit should come from A's most significant digit
- C's second digit should come from B's most significant digit
- C's third digit should come from A's second digit
- C's third digit should come from B's second digit
- This goes until one of the numbers runs out of digits. Then just add the rest of the other decimal.
- If the result is bigger than 10.000.000 the result should be -1 otherwise the zipped result.

### Examples ###
A = 123, B = 20 => C = 12203

A = 12345, B = 1 => C = 112345

## Property based testing ##
[Property based testing](https://github.com/rickynils/scalacheck/wiki/User-Guide) is an extremely good feature. I believe it is really neglected testing form despite the fact that it is very powerful.

### Test example ###
- Define a generator for decimal inputs between 0 and 100.000.000
- Define a test where the generator is used. Here we use the decimal generator for the two decimals we need for the **solution** method
- The generator will generate 100 different inputs (configurable) and run the test scenario
- As it is randomly picks numbers provides better coverage and helps to cover edge cases
- The classify method collects the generated decimals and provides some statistics about the generated items

```scala
  val n = Gen.chooseNum(0, 100000000)

  property("ends") = Prop.forAll(n, n) {
    (n, m) =>
      classify(n.toString.length != m.toString.length, "not equal") {
        val result = solution.solution(n, m)
        if ((n.toString + m.toString).toLong <= 100000000) {
          if (n.toString.length > m.toString.length) result.toString endsWith n.toString.substring(m.toString.length)
          else result.toString endsWith m.toString.substring(n.toString.length)
        } else {
          result == -1
        }
      }
  }
```

### Result ###
```
+ zip.ends: OK, passed 100 tests.
> Collected test data:
58% not equal
+ zip.position: OK, passed 100 tests.
> Collected test data:
47% big, short
41% big
12% short
+ zip.first zero: OK, passed 100 tests.
+ zip.zero zero: OK, passed 100 tests.
+ zip.bigger than 100.000.000: OK, passed 100 tests.
> Collected test data:
71% big
```

