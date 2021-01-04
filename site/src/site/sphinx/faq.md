
## FAQ


> 不在本列表里的问题，请到issue里搜索。 [https://github.com/alibaba/arthas/issues](https://github.com/alibaba/arthas/issues)

##### Arthas attach之后对原进程性能有多大的影响

[https://github.com/alibaba/arthas/issues/44](https://github.com/alibaba/arthas/issues/44)


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