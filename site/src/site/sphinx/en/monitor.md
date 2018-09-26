monitor
=======

Monitor methods calling stack traces.

F.Y.I

1. `monitor` is a persistent command, it never returns until you press `Ctrl+C` to manually stop it;
2. the server runs the jobs in the background;
3. injected monitoring code will become invalid automatically once the monitoring jobs being terminated;
4. in theory, Arthas will not change any original behaviors but if it does, please do not hesitate to start an [issue](https://github.com/alibaba/arthas/issues).

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
|[E]|turn on regex matching while the default is wildcard matching|
|[c:]|cycle of output with default value: `60 s`|

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
