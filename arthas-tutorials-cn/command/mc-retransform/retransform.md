
> 加载外部的`.class`文件，retransform jvm已加载的类。

参考：[Instrumentation#retransformClasses](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses-java.lang.Class...-)


### 参数说明

|参数名称|参数说明|
|---:|:---|
|[c:]|ClassLoader的hashcode|
|`[classLoaderClass:]`|指定执行表达式的 ClassLoader 的 class name|
|[p:]|外部的`.class`文件的完整路径，支持多个|


### retransform的限制

* 不允许新增加field/method
* 正在跑的函数，没有退出不能生效。
