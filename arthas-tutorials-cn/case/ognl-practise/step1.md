
演示Arthas里`watch`命令中`ognl`表达式的工作流程。用户可以自己修改`Demo.java`里的表达式，再执行验证。

项目地址： https://github.com/hengyunabc/ognl-demo
# 打开Demo.java文件

`src/main/java/com/example/ognl/Demo.java`{{open}}

# 编译运行代码

`mvn compile exec:java`{{execute}}


输出结果包含`ognl`表达式输出结果：

```java
AtEnter, conditionExpress: params[0] > 1, conditionResult: true
@ArrayList[
    @TestService[
    ],
    @Object[][
        @Integer[1000],
        @String[hello],
        @Student[
            id=@Long[1],
            name=@String[tom],
        ],
    ],
]
```


以上输出结果，对应在代码里的表达式是：

```java
String watchExpress = "{target, params, returnObj, #cost}";
String conditionExpress = "params[0] > 1 && #cost > 0.1";
```

类似在arthas里执行下面的`watch`命令：

```bash
watch com.example.ognl.TestService test "{target, params, returnObj, #cost}" "params[0] > 1 && #cost > 0.1" -x 3
```

# 查看函数抛出异常时的表达式结果

`mvn compile exec:java -DexceptionCase=true`{{execute}}


The output:

```java
AtExceptionExit, conditionExpress: params[0] > 1, conditionResult: true
@ArrayList[
    @TestService[
    ],
    @Object[][
        @Integer[1000],
        @String[hello],
        @Student[com.example.ognl.Student@6e23bcdd],
    ],
    java.lang.IllegalArgumentException: error
        at com.example.ognl.TestService.test(TestService.java:12)
        at com.example.ognl.Demo.test(Demo.java:43)
        at com.example.ognl.Demo.main(Demo.java:20)
        at org.codehaus.mojo.exec.ExecJavaMojo$1.run(ExecJavaMojo.java:254)
        at java.base/java.lang.Thread.run(Thread.java:832)
,
]
```

代码里的表达式：

```java
String watchExpress = "{target, params, throwExp}";
String conditionExpress = "params[0] > 1";
```

类似在arthas里执行下面的`watch`命令：

```bash
watch com.example.ognl.TestService test "{target, params, throwExp}" "params[0] > 1" -e -x 2
```
