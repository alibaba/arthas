
Demonstrate the workflow of the `ognl` expression in the `watch` command in Arthas. You can modify the expressions in `Demo.java`, and compile and run the code.

Project: https://github.com/hengyunabc/ognl-demo

# Open the Demo.java file

`src/main/java/com/example/ognl/Demo.java`{{open}}

# Compile and run the code

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


Expression in the code:

```java
String watchExpress = "{target, params, returnObj, #cost}";
String conditionExpress = "params[0] > 1 && #cost > 0.1";
```

The result is similar to the following expression:

```bash
watch com.example.ognl.TestService test "{target, params, returnObj, #cost}" "params[0] > 1 && #cost > 0.1" -x 3
```

# View the expression result when the method throws an exception

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

Expression in the code:

```java
String watchExpress = "{target, params, throwExp}";
String conditionExpress = "params[0] > 1";
```

The result is similar to the following expression:

```bash
watch com.example.ognl.TestService test "{target, params, throwExp}" "params[0] > 1" -e -x 2
```
