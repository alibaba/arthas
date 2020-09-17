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
|*condition-expression*|condition expression for filtering method calls|
|`[E]`|turn on regex matching while the default is wildcard matching|
|`[c:]`|cycle of statistics, the default value: `120`s|
|`[b]`|evaluate the condition-expression before method invoke|

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

#### Evaluate condition-express to filter method (after method call)

```bash
monitor -c 5 demo.MathGame primeFactors "params[0] <= 2"
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 19 ms, listenerId: 5
 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
-----------------------------------------------------------------------------------------------
 2020-09-02 09:42:36  demo.MathGame  primeFactors    5       3       2      0.09       40.00%           

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:42:41  demo.MathGame  primeFactors    5       2       3      0.11       60.00%           

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:42:46  demo.MathGame  primeFactors    5       1       4      0.06       80.00%           

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:42:51  demo.MathGame  primeFactors    5       1       4      0.12       80.00%           

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:42:56  demo.MathGame  primeFactors    5       3       2      0.15       40.00%           
```

#### Evaluate condition-express to filter method (before method call)

```bash
monitor -b -c 5 com.test.testes.MathGame primeFactors "params[0] <= 2"
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 21 ms, listenerId: 4
 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:41:57  demo.MathGame  primeFactors    1       0        1      0.10      100.00%          

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:42:02  demo.MathGame  primeFactors    3       0        3      0.06      100.00%  

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:42:07  demo.MathGame  primeFactors    2       0        2      0.06      100.00% 

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:42:12  demo.MathGame  primeFactors    1       0        1      0.05      100.00% 

 timestamp            class          method         total  success  fail  avg-rt(ms)  fail-rate        
----------------------------------------------------------------------------------------------
 2020-09-02 09:42:17  demo.MathGame  primeFactors    2       0        2      0.10      100.00% 
```

