redefine
===

> 加载外部的`.class`文件，redefine jvm已加载的类。

参考：[Instrumentation#redefineClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses-java.lang.instrument.ClassDefinition...-)

> 注意， redefine后的原来的类不能恢复，redefine有可能失败（比如增加了新的field），参考jdk本身的文档。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|[c:]|ClassLoader的hashcode|
|[p:]|外部的`.class`文件的完整路径，支持多个|



### 使用参考

```
   redefine -p /tmp/Test.class
   redefine -c 327a647b -p /tmp/Test.class /tmp/Test\$Inner.class
```