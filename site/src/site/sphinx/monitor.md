monitor
=======

> 方法执行监控

对匹配 `class-pattern`／`method-pattern`的类、方法的调用进行监控。

`monitor` 命令是一个非实时返回命令.

实时返回命令是输入之后立即返回，而非实时返回的命令，则是不断的等待目标 Java 进程返回信息，直到用户输入 `Ctrl+C` 为止。

服务端是以任务的形式在后台跑任务，植入的代码随着任务的中止而不会被执行，所以任务关闭后，不会对原有性能产生太大影响，而且原则上，任何Arthas命令不会引起原有业务逻辑的改变。

### 监控的维度说明

|监控项|说明|
|---:|:---|
|timestamp|时间戳|
|class|Java类|
|method|方法（构造方法、普通方法）|
|total|调用次数|
|success|成功次数|
|fail|失败次数|
|rt|平均RT|
|fail-rate|失败率|

### 参数说明

方法拥有一个命名参数 `[c:]`，意思是统计周期（cycle of output），拥有一个整型的参数值

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|[E]|开启正则表达式匹配，默认为通配符匹配|
|`[c:]`|统计周期，默认值为120秒|

### 使用参考

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
