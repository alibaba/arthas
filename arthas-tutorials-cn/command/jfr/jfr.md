::: tip
Java Flight Recorder (JFR) 是一种用于收集有关正在运行的 Java 应用程序的诊断和分析数据的工具。它集成到 Java 虚拟机 (JVM) 中，几乎不会造成性能开销，因此即使在负载较重的生产环境中也可以使用。
:::

[jfr 命令](https://arthas.aliyun.com/doc/jfr.html)支持在程序动态运行过程中开启和关闭 JFR 记录。记录收集有关 event 的数据。事件在特定时间点发生在 JVM 或 Java 应用程序中。每个事件都有一个名称、一个时间戳和一个可选的有效负载。负载是与事件相关的数据，例如 CPU 使用率、事件前后的 Java 堆大小、锁持有者的线程 ID 等。

`jfr` 命令基本运行结构是 `jfr cmd [actionArg]`

> 注意：JDK8 的 8u262 版本之后才支持 jfr

## 启动 JFR 记录

`jfr start`{{execute T2}}

```
$ jfr start
Started recording 1. No limit specified, using maxsize=250MB as default.
```

::: tip
开启的是默认参数的 jfr 记录
:::

启动 jfr 记录，指定记录名，记录持续时间，记录文件保存路径。

`jfr start -n myRecording --duration 60s -f /tmp/myRecording.jfr`{{execute T2}}

## 查看 JFR 记录状态

默认是查看所有 JFR 记录信息

`jfr status`{{execute T2}}

查看指定记录 id 的记录信息

`jfr status -r 1`{{execute T2}}

查看指定状态的记录信息

`jfr status --state closed`{{execute T2}}

## dump jfr 记录

指定记录输出路径

`$ jfr dump -r 1 -f /tmp/myRecording1.jfr`{{execute T2}}

不指定文件输出路径，默认是保存到`arthas-output`目录下

`jfr dump -r 1`{{execute T2}}

## 停止 jfr 记录

不指定记录输出路径，默认是保存到`arthas-output`目录下

`jfr stop -r 1`{{execute T2}}

> 注意一条记录只能停止一次。

也可以指定记录输出路径。

## 通过浏览器查看 arthas-output 下面 JFR 记录的结果

默认情况下，arthas 使用 8563 端口，则可以打开： [http://localhost:8563/arthas-output/](http://localhost:8563/arthas-output/) 查看到`arthas-output`目录下面的 JFR 记录结果：

![](/images/arthas-output-recording.png)

生成的结果可以用支持 jfr 格式的工具来查看。比如：

- JDK Mission Control : https://github.com/openjdk/jmc
