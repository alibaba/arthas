### 现象

目前，访问 [/user/0]({{TRAFFIC_HOST1_80}}/user/0) ，会返回 500 异常：

但请求的具体参数，异常栈是什么呢？

### 查看 UserController 的 参数/异常

在 Arthas 里执行：

`watch com.example.demo.arthas.user.UserController * '{params, throwExp}'`{{execute T2}}

1. 第一个参数是类名，支持通配
2. 第二个参数是函数名，支持通配

访问 [/user/0]({{TRAFFIC_HOST1_80}}/user/0) ,`watch`命令会打印调用的参数和异常

可以看到实际抛出的异常是`IllegalArgumentException`。

可以输入 `Q`{{exec interrupt}} 或者 `Ctrl+C`{{exec interrupt}} 退出 watch 命令。

如果想把获取到的结果展开，可以用`-x`参数：

`watch com.example.demo.arthas.user.UserController * '{params, throwExp}' -x 2`{{execute T2}}

### 返回值表达式

在上面的例子里，第三个参数是`返回值表达式`，它实际上是一个`ognl`表达式，它支持一些内置对象：

- loader
- clazz
- method
- target
- params
- returnObj
- throwExp
- isBefore
- isThrow
- isReturn

你可以利用这些内置对象来组成不同的表达式。比如返回一个数组：

`watch com.example.demo.arthas.user.UserController * '{params[0], target, returnObj}'`{{execute T2}}

更多参考：https://arthas.aliyun.com/doc/advice-class.html

### 条件表达式

`watch`命令支持在第 4 个参数里写条件表达式，比如：

`watch com.example.demo.arthas.user.UserController * returnObj 'params[0] > 100'`{{execute T2}}

当访问 [/user/1]({{TRAFFIC_HOST1_80}}/user/1) 时，`watch`命令没有输出

当访问 [/user/101]({{TRAFFIC_HOST1_80}}/user/101) 时，`watch`会打印出结果。

### 当异常时捕获

`watch`命令支持`-e`选项，表示只捕获抛出异常时的请求：

`watch com.example.demo.arthas.user.UserController * "{params[0],throwExp}" -e`{{execute T2}}

### 按照耗时进行过滤

watch 命令支持按请求耗时进行过滤，比如：

`watch com.example.demo.arthas.user.UserController * '{params, returnObj}' '#cost>200'`{{execute T2}}
