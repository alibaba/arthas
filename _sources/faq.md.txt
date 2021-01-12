
## FAQ


> 不在本列表里的问题，请到issue里搜索。 [https://github.com/alibaba/arthas/issues](https://github.com/alibaba/arthas/issues)

##### Arthas attach之后对原进程性能有多大的影响

[https://github.com/alibaba/arthas/issues/44](https://github.com/alibaba/arthas/issues/44)


##### trace/watch等命令能否增强jdk里的类？

默认情况下会过滤掉`java.`开头的类，但可以通过参数开启。

```bash
options unsafe true
```

更多参考 [options](options.md)

##### 怎么以`json`格式查看结果

```bash
options json-format true
```

更多参考 [options](options.md)


##### Arthas能否跟踪 native 函数

不能。


##### 能不能查看内存里某个变量的值

不能。但可以用一些技巧，用`tt`命令拦截到对象，或者从静态函数里取到对象。


##### 方法同名过滤

同名方法过滤可以通过匹配表达式,可以使用[表达式核心变量](advice-class.md)中所有变量作为已知条件,可以通过判断参数个数`params.length ==1`, 参数类型`params[0] instanceof java.lang.Integer`、返回值类型 `returnObj instanceof java.util.List` 等等一种或者多种组合进行过滤。

可以使用 `-v` 查看观察匹配表达式的执行结果 [https://github.com/alibaba/arthas/issues/1348](https://github.com/alibaba/arthas/issues/1348)

例子[arthas-demo](quick-start.md)

```bash
watch demo.MathGame primeFactors traceE '{params,returnObj,throwExp}' -v -n 5 -x 3 'params.length >0 && returnObj instanceof java.util.List'
``` 

##### 怎么watch、trace 构造函数 ？

```bash
watch demo.MathGame <init> '{params,returnObj,throwExp}' -v -n 5 -x 3 '1==1'
```


##### java.lang.ClassFormatError: null、skywalking arthas 兼容使用

当出现这个错误日志`java.lang.ClassFormatError: null`,通常情况下都是被其他字节码工具修改过与arthas修改字节码不兼容。

比如: 使用 skywalking V8.1.0 以下版本 [无法trace、watch 被skywalking agent 增强过的类](https://github.com/alibaba/arthas/issues/1141), V8.1.0 以上版本可以兼容使用,更多参考skywalking配置 [skywalking compatible with other javaagent bytecode processing](https://github.com/apache/skywalking/blob/v8.1.0/docs/en/FAQ/Compatible-with-other-javaagent-bytecode-processing.md)。

