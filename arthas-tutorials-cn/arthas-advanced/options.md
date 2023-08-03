在 Arthas 里有一些开关，可以通过 `options`{{execute T2}} 命令来查看 - [options 命令文档](https://arthas.aliyun.com/doc/options.html)。

查看单个 option 的值，比如

`options unsafe`{{execute T2}}

## 允许增强 JDK 的类

默认情况下`unsafe`为 false，即 watch/trace 等命令不会增强 JVM 的类，即`java.*`下面的类。

如果想增强 JVM 里的类，可以执行 `options unsafe true`{{execute T2}} ，设置`unsafe`为 true。

## 以 JSON 格式打印对象

当 `json-format` 为 false 时，输出结果是：

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'
@ArrayList[
    @String[/usr/lib/jvm/java-8-oracle/jre],
    @String[Java(TM) SE Runtime Environment],
]
```

`options json-format true`{{execute T2}}

当 `json-format` 为 true 时，输出结果是：

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#v["/usr/lib/jvm/java-8-oracle/jre","Java(TM) SE Runtime Environment"]
```
