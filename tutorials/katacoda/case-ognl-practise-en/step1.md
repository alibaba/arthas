
Demonstrate the workflow of the `ognl` expression in the `watch` command in Arthas. You can modify the expressions in `Demo.java`, and compile and run the code.

Project: https://github.com/hengyunabc/ognl-demo

# Open the Demo.java file

`src/main/java/com/example/ognl/Demo.java`{{open}}

# Compile and run the code

`mvn compile exec:java`{{execute}}

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

Expression in the code:

```java
String watchExpress = "{target, params, throwExp}";
String conditionExpress = "params[0] > 1";
```

The result is similar to the following expression:

```bash
watch com.example.ognl.TestService test "{target, params, throwExp}" "params[0] > 1" -e -x 2
```
