Fundamental Fields in Expressions
==============================

There is a very fundamental class `Advice` for the expressions used in filtering, tracing or monitoring and other aspects in commands.  

```java
public class Advice {

    private final ClassLoader loader;
    private final Class<?> clazz;
    private final ArthasMethod method;
    private final Object target;
    private final Object[] params;
    private final Object returnObj;
    private final Throwable throwExp;
    private final boolean isBefore;
    private final boolean isThrow;
    private final boolean isReturn;
    
    // getter/setter  
}  
```

Description for the variables in the class `Advice`:

|Name|Specification|
|---:|:---|
|loader|the class loader for the current called class|
|clazz|the reference to the current called class|
|method|the reference to the current called method|
|target|the instance of the current called class|
|params|the parameters for the current call, which is an array (when there's no parameter, it will be an empty array)|
|returnObj|the return value from the current call - only available when the method call returns normally (`isReturn==true`), and `null` is for `void` return value|
|throwExp|the exceptions thrown from the current call - only available when the method call throws exception (`isThrow==true`)|
|isBefore|flag to indicate the method is about to execute. `isBefore==true` but `isThrow==false` and `isReturn==false` since it's no way to know how the method call will end|
|isThrow|flag to indicate the method call ends with exception thrown|
|isReturn|flag to indicate the method call ends normally without exception thrown|

All variables listed above can be used directly in the [OGNL expression](https://commons.apache.org/proper/commons-ognl/language-guide.html). The command will not execute and exit if there's illegal OGNL grammar or unexpected variable in the expression.
* [typical use cases](https://github.com/alibaba/arthas/issues/71);
* [OGNL language guide](https://commons.apache.org/proper/commons-ognl/language-guide.html).



