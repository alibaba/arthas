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

```shell
$ monitor -c 5 com.alibaba.sample.petstore.web.store.module.screen.ItemList execute
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 36 ms.
 timestamp            class                                                         method   total  success  fail  rt    fail-rate
-----------------------------------------------------------------------------------------------------------------------------------
 2015-12-17 10:56:40  com.alibaba.sample.petstore.web.store.module.screen.ItemList  execute  10     10       0     2.00  0.00%

 timestamp            class                                                         method   total  success  fail  rt    fail-rate
-----------------------------------------------------------------------------------------------------------------------------------
 2015-12-17 10:56:45  com.alibaba.sample.petstore.web.store.module.screen.ItemList  execute  11     11       0     2.18  0.00%

 timestamp            class                                                         method   total  success  fail  rt    fail-rate
-----------------------------------------------------------------------------------------------------------------------------------
 2015-12-17 10:56:50  com.alibaba.sample.petstore.web.store.module.screen.ItemList  execute  0      0        0     0.00  0.00%
```
