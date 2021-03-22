
演示Arthas里`watch`命令中`ognl`表达式的工作流程。用户可以自己修改`Demo.java`里的表达式，再执行验证。

项目地址： https://github.com/hengyunabc/ognl-demo
# 打开Demo.java文件

`src/main/java/com/example/ognl/Demo.java`{{open}}

# 编译运行代码

`mvn compile exec:java`{{execute}}

代码里的表达式：

```java
String watchExpress = "{target, params, returnObj, #cost}";
String conditionExpress = "params[0] > 1 && #cost > 0.1";
```

结果类似下面的表达式：

```bash
watch com.example.ognl.TestService test "{target, params, returnObj, #cost}" "params[0] > 1 && #cost > 0.1" -x 3
```

# 查看函数抛出异常时的表达式结果

`mvn compile exec:java -DexceptionCase=true`{{execute}}

代码里的表达式：

```java
String watchExpress = "{target, params, throwExp}";
String conditionExpress = "params[0] > 1";
```

结果类似下面的表达式：

```bash
watch com.example.ognl.TestService test "{target, params, throwExp}" "params[0] > 1" -e -x 2
```
