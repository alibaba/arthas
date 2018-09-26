Critical Fields in Expressions
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
    ...
    // getter/setter  
}  
```

|Name|Specification|
|---:|:---|
|loader|class loader of the class|
|clazz|the reference of the class|
|method|the reflective reference of the method|
|target|the instance of the class|
|params|the parameters of the method, which is an array (when there is no argument in the method, it will be an empty array)|
|returnObj|the return value of the method - only when `isReturn==true`, it's a valid result but if the return value is `void` then it will be a `null`|
|throwExp|the exceptions thrown by the method invoking - only when `isThrow==true`, it's a valid thrown exception|
|isBefore|assistant checking flag used in [`before-watching` point](watch.md)  and at this very moment: `isBefore==true`, `isThrow==false` and `isReturn==false` since it's before we invoke the method|
|isThrow|assistant checking flag: whether the current method invoking ends with exceptions|
|isReturn|assistant checking flag: whether the method invoking exits normally without exceptions|

F.Y.I
1. all the *fields* mentioned in the table above can be used directly in the `expressions`; 
2. if the expressions are [invalid OGNL](https://en.wikipedia.org/wiki/OGNL), the command will be cancelled automatically with hints to correct the expressions;
3. [typical use cases](https://github.com/alibaba/arthas/issues/71);
4. [OGNL official usage guide](https://commons.apache.org/proper/commons-ognl/language-guide.html).



