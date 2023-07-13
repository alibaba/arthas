


在Arthas里有一些开关，可以通过 `options`{{execute T2}} 命令来查看。


查看单个option的值，比如

`options unsafe`{{execute T2}}


## 允许增强JDK的类

默认情况下`unsafe`为false，即watch/trace等命令不会增强JVM的类，即`java.*`下面的类。

如果想增强JVM里的类，可以执行 `options unsafe true`{{execute T2}} ，设置`unsafe`为true。


## 以JSON格式打印对象

当 `json-format` 为false时，输出结果是：

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'
@ArrayList[
    @String[/usr/lib/jvm/java-8-oracle/jre],
    @String[Java(TM) SE Runtime Environment],
]
```

`options json-format true`{{execute T2}}

当 `json-format` 为true时，输出结果是：

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#v["/usr/lib/jvm/java-8-oracle/jre","Java(TM) SE Runtime Environment"]
```