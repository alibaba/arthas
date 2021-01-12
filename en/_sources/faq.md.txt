## FAQ


> For questions that are not in this list, please search in issues. [https://github.com/alibaba/arthas/issues](https://github.com/alibaba/arthas/issues)

##### How much impact does Arthas attach have on the performance of the original process?

[https://github.com/alibaba/arthas/issues/44](https://github.com/alibaba/arthas/issues/44)


##### Can commands such as trace/watch enhance the classes in jdk?

By default, classes beginning with `java.` are filtered out, but they can be turned on:

```bash
options unsafe true
```

See more at [options](options.md)


##### How to view the result in `json` format

```bash
options json-format true
```

See more at [options](options.md)


##### Can arthas trace native methods

No.

##### Can arthas view the value of a variable in memory?

No. But you can use some tricks to intercept the object with the `tt` command, or fetch it from a static method.


##### How to filter method with the same name?

You can used all variables in [fundamental fields in expressions](advice-class.md) for the condition express to filter method with the same name, you can use the number of parameters `params.length ==1`,parameter type `params[0] instanceof java.lang.Integer`,return value type `returnObj instanceof java.util.List` and so on in one or more combinations as condition express.

You can use `-v` to view the condition express result [https://github.com/alibaba/arthas/issues/1348](https://github.com/alibaba/arthas/issues/1348)

example [arthas-demo](quick-start.md)

```bash
watch demo.MathGame primeFactors traceE '{params,returnObj,throwExp}' -v -n 5 -x 3 'params.length >0 && returnObj instanceof java.util.List'
``` 

##### How to watch or trace constructor?

```bash
watch demo.MathGame <init> '{params,returnObj,throwExp}' -v -n 5 -x 3 '1==1'
```


##### java.lang.ClassFormatError: null, skywalking arthas compatible use

When error log appear `java.lang.ClassFormatError: null`, it is usually modified by other bytecode tools that are not compatible with arthas modified bytecode.

For example: use skywalking V8.1.0 below [cannot trace, watch classes enhanced by skywalking agent](https://github.com/alibaba/arthas/issues/1141), V8.1.0 or above is compatible, refer to skywalking configuration for more details. [skywalking compatible with other javaagent bytecode processing](https://github.com/apache/skywalking/blob/v8.1.0/docs/en/FAQ/Compatible-with-other-javaagent-bytecode-processing.md).