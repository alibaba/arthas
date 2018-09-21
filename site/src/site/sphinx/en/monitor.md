monitor
=======

Monitor the `class-pattern` & `method-pattern` matched methods invoking traces.

F.Y.I

1. `monitor` is a persistent command, it never returns until `Ctrl+C` to manually stop it. 
2. the server runs the tasks in the background;
3. injected code will become invalid automatically once the tasks being terminated;
4. in theory, Arthas commands will not change any original behaviors.

### Properties monitored

|Property|Specification|
|---:|:---|
|timestamp|timestamp|
|class|Java class|
|method|constructor and regular methods|
|total|calling times|
|success|success count|
|fail|failure count|
|rt|average RT|
|fail-rate|failure ratio|

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|[E]|turn on regx matching while the default is wildcards matching|
|`[c:]`|cycle of output with default value: `120 s`|

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
