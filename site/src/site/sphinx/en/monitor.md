monitor
=======

[`monitor` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-monitor)

> Monitor method invocation.

Monitor invocation for the method matched with `class-pattern` and `method-pattern` and filter by `condition-expression`.

`monitor` is not a command returning immediately.

A command returning immediately is a command immediately returns with the result after the command is input, while a non-immediate returning command will keep outputting the information from the target JVM process until user presses `Ctrl+C`.

On Arthas's server side, the command is running as a background job, but the weaved code will not take further effect once the job is terminated, therefore, it will not impact the performance after the job quits. Furthermore, Arthas is designed to have no side effect to the business logic.

### Items to monitor

|Item|Specification|
|---:|:---|
|timestamp|timestamp|
|class|Java class|
|method|method (constructor and regular methods)|
|total|calling times|
|success|success count|
|fail|failure count|
|rt|average RT|
|fail-rate|failure ratio|

### Parameters

Parameter `[c:]` stands for cycles of statistics. Its value is an integer value in seconds.

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-expression*|condition expression to filter|
|`[E]`|turn on regex matching while the default is wildcard matching|
|`[c:]`|cycle of statistics, the default value: `120`s|
|`[b]`|execute condition-express before method being invoked|

### Usage

```bash
$ monitor -c 5 demo.MathGame primeFactors
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 94 ms.
 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:38  demo.MathGame  primeFactors  5      1        4     1.15        80.00%

 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:43  demo.MathGame  primeFactors  5      3        2     42.29       40.00%

 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:48  demo.MathGame  primeFactors  5      3        2     67.92       40.00%

 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:53  demo.MathGame  primeFactors  5      2        3     0.25        60.00%

 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:06:58  demo.MathGame  primeFactors  1      1        0     0.45        0.00%

 timestamp            class          method        total  success  fail  avg-rt(ms)  fail-rate
-----------------------------------------------------------------------------------------------
 2018-12-03 19:07:03  demo.MathGame  primeFactors  2      2        0     3182.72     0.00%
```

#### Example for use condition-express
```bash
$ monitor -c 5 demo.MathGame primeFactors -b "params[0] >= 2"
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 79 ms, listenerId: 1
 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate   
-----------------------------------------------------------------------------------------------
 2020-08-17 16:25:30  demo.MathGame  primeFactors   3      3        0     18.99       0.00%       

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate   
-----------------------------------------------------------------------------------------------
 2020-08-17 16:25:35  demo.MathGame  primeFactors   3      3        0     1.02        0.00%       

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate   
-----------------------------------------------------------------------------------------------
 2020-08-17 16:25:40  demo.MathGame  primeFactors   3      3        0     0.74        0.00%       

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate   
-----------------------------------------------------------------------------------------------
 2020-08-17 16:25:45  demo.MathGame  primeFactors   2      2        0     1.94        0.00%
```
