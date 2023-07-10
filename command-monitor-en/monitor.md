monitor
=======

> Monitor method invocation.

Monitor invocation for the method matched with `class-pattern` and `method-pattern`.

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
|`[E]`|turn on regex matching while the default is wildcard matching|
|`[c:]`|cycle of statistics, the default value: `120`s|

### Usage

`monitor -c 5 demo.MathGame primeFactors`{{execute T2}}

Press `Q`{{execute T2}} or `Ctrl+C` to abort

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
