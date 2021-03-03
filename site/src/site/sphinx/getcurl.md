
> 捕获Http请求并转化为Curl命令

让你能方便的捕获到指定方法的请求并转化为Curl命令，通过Curl命令可以快速在终端或者导入Postman进行接口调试及问题复现。其基本支持与 `watch` 命令一致的条件表达式，可以用于限定捕获的请求用户id等。

**注意：** 捕获Curl适配的Spring web框架，所有基于Spring web框架的请求处理链路方法都可以捕获请求并转化为Curl。

### 参数说明

`getcurl` 的参数比较多，用途基本与watch一致。

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|*condition-express*|条件表达式|
|[b]|在**方法调用之前**观察|
|[e]|在**方法异常之后**观察|
|[s]|在**方法返回之后**观察|
|[f]|在**方法结束之后**(正常返回和异常返回)观察|
|[E]|开启正则表达式匹配，默认为通配符匹配|

### 使用参考

#### 捕获请求Curl

传递`-n 5`可以进行5次捕获

```bash
[arthas@16992]$ getcurl demo.CurlTestRest testMethod -n 5
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 79 ms, listenerId: 1
ts=2021-03-03 15:52:08
method=demo.CurlRest.testLogCurlPost
curl=[ curl --location -X POST -H "user-agent:PostmanRuntime/7.26.8" -H "accept:*/*" -H "postman-token:09847e92-e0c8-4a25-bd0e-ab75c3c4f4a7" -H "host:localhost:10000" -H "accept-encoding:gzip, deflate, br" -H "connection:keep-alive" -d '' "http://localhost:10000/testLogCurl?val=1" ]

```

#### 条件表达式的例子

可以基于条件表达式来限定捕获条件，例如可以基于方法传递的用户id进行过滤

```bash
[arthas@16992]$ getcurl demo.CurlTestRest testMethod "params[0]==100"
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 79 ms, listenerId: 1
ts=2021-03-03 15:52:08
method=demo.CurlRest.testLogCurlPost
curl=[ curl --location -X POST -H "user-agent:PostmanRuntime/7.26.8" -H "accept:*/*" -H "postman-token:09847e92-e0c8-4a25-bd0e-ab75c3c4f4a7" -H "host:localhost:10000" -H "accept-encoding:gzip, deflate, br" -H "connection:keep-alive" -d '' "http://localhost:10000/testLogCurl?val=1" ]
```

#### 异常时捕获的例子

添加参数 `-e` ，当方法报异常时将会输出请求Curl

```bash
[arthas@16992]$ getcurl demo.CurlTestRest testMethod -e
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 79 ms, listenerId: 1
ts=2021-03-03 15:52:08
method=demo.CurlRest.testLogCurlPost
curl=[ curl --location -X POST -H "user-agent:PostmanRuntime/7.26.8" -H "accept:*/*" -H "postman-token:09847e92-e0c8-4a25-bd0e-ab75c3c4f4a7" -H "host:localhost:10000" -H "accept-encoding:gzip, deflate, br" -H "connection:keep-alive" -d '' "http://localhost:10000/testLogCurl?val=1" ]
```

