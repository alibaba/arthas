表达式核心变量
===

无论是匹配表达式也好、观察表达式也罢，他们核心判断变量都是围绕着一个 Arthas 中的通用通知对象 `Advice` 进行。

它的简略代码结构如下
  
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

这里列一个表格来说明不同变量的含义

|变量名|变量解释|
|---:|:---|
|loader|本次调用类所在的 ClassLoader|
|clazz|本次调用类的 Class 引用|
|method|本次调用方法反射引用|
|target|本次调用类的实例|
|params|本次调用参数列表，这是一个数组，如果方法是无参方法则为空数组|
|returnObj|本次调用返回的对象。当且仅当 `isReturn==true` 成立时候有效，表明方法调用是以正常返回的方式结束。如果当前方法无返回值 `void`，则值为 null|
|throwExp|本次调用抛出的异常。当且仅当 `isThrow==true` 成立时有效，表明方法调用是以抛出异常的方式结束。|
|isBefore|辅助判断标记，当前的通知节点有可能是在方法一开始就通知，此时 `isBefore==true` 成立，同时 `isThrow==false` 和 `isReturn==false`，因为在方法刚开始时，还无法确定方法调用将会如何结束。|
|isThrow|辅助判断标记，当前的方法调用以抛异常的形式结束。|
|isReturn|辅助判断标记，当前的方法调用以正常返回的形式结束。|

所有变量都可以在表达式中直接使用，如果在表达式中编写了不符合 OGNL 脚本语法或者引入了不在表格中的变量，则退出命令的执行；用户可以根据当前的异常信息修正`条件表达式`或`观察表达式`

* 特殊用法请参考：[https://github.com/alibaba/arthas/issues/71](https://github.com/alibaba/arthas/issues/71)
* OGNL表达式官网：[https://commons.apache.org/proper/commons-ognl/language-guide.html](https://commons.apache.org/proper/commons-ognl/language-guide.html)



