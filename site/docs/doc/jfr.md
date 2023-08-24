# jfr

[`jfr`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-jfr)

::: tip
Java Flight Recorder (JFR) 是一种用于收集有关正在运行的 Java 应用程序的诊断和分析数据的工具。它集成到 Java 虚拟机 (JVM) 中，几乎不会造成性能开销，因此即使在负载较重的生产环境中也可以使用。
:::

`jfr` 命令支持在程序动态运行过程中开启和关闭 JFR 记录。 记录收集有关 event 的数据。事件在特定时间点发生在 JVM 或 Java 应用程序中。每个事件都有一个名称、一个时间戳和一个可选的有效负载。负载是与事件相关的数据，例如 CPU 使用率、事件前后的 Java 堆大小、锁持有者的线程 ID 等。

`jfr` 命令基本运行结构是 `jfr cmd [actionArg]`

> 注意： JDK8 的 8u262 版本之后才支持 jfr

## 参数说明

|      参数名称 | 参数说明                                                                                  |
| ------------: | :---------------------------------------------------------------------------------------- |
|         _cmd_ | 要执行的操作 支持的命令【start，status，dump，stop】                                      |
|   _actionArg_ | 属性名模式                                                                                |
|          [n:] | 记录名称                                                                                  |
|          [r:] | 记录 id 值                                                                                |
| [dumponexit:] | 程序退出时，是否要 dump 出 .jfr 文件，默认为 false                                        |
|          [d:] | 延迟多久后启动 JFR 记录，支持带单位配置，eg: 60s, 2m, 5h, 3d. 不带单位就是秒，默认无延迟  |
|   [duration:] | JFR 记录持续时间，支持单位配置，不带单位就是秒，默认一直记录                              |
|          [s:] | 采集 Event 的详细配置文件，默认是 default.jfc 位于 `$JAVA_HOME/lib/jfr/default.jfc`       |
|          [f:] | 将输出转储到指定路径                                                                      |
|     [maxage:] | 缓冲区数据最大文件记录保存时间，支持单位配置，不带单位就是秒，默认是不限制                |
|    [maxsize:] | 缓冲区的最大文件大小，支持单位配置， 不带单位是字节，m 或者 M 代表 MB，g 或者 G 代表 GB。 |
|      [state:] | jfr 记录状态                                                                              |

## 启动 JFR 记录

```
$ jfr start
Started recording 1. No limit specified, using maxsize=250MB as default.
```

::: tip
默认情况下，开启的是默认参数的 jfr 记录
:::

启动 jfr 记录，指定记录名，记录持续时间，记录文件保存路径。

```
$ jfr start -n myRecording --duration 60s -f /tmp/myRecording.jfr
Started recording 2. The result will be written to:
/tmp/myRecording.jfr
```

## 查看 JFR 记录状态

默认是查看所有 JFR 记录信息

```bash
$ jfr status
Recording: recording=1 name=Recording-1 (running)
Recording: recording=2 name=myRecording duration=PT1M (closed)
```

查看指定记录 id 的记录信息

```bash
$ jfr status -r 1
Recording: recording=1 name=Recording-1 (running)
```

查看指定状态的记录信息

```bash
$ jfr status --state closed
Recording: recording=2 name=myRecording duration=PT1M (closed)
```

## dump jfr 记录

`jfr dump`{{}} 会输出从开始到运行该命令这段时间内的记录到 JFR 文件，且不会停止 `jfr`{{}} 的记录  
指定记录输出路径

```bash
$ jfr dump -r 1 -f /tmp/myRecording1.jfr
Dump recording 1, The result will be written to:
/tmp/myRecording1.jfr
```

不指定文件输出路径，默认是保存到`arthas-output`目录下

```bash
$ jfr dump -r 1
Dump recording 1, The result will be written to:
/tmp/test/arthas-output/20220819-200915.jfr
```

## 停止 jfr 记录

不指定记录输出路径，默认是保存到`arthas-output`目录下

```bash
$ jfr stop -r 1
Stop recording 1, The result will be written to:
/tmp/test/arthas-output/20220819-202049.jfr
```

> 注意一条记录只能停止一次。

也可以指定记录输出路径。

## 通过浏览器查看 arthas-output 下面 JFR 记录的结果

默认情况下，arthas 使用 8563 端口，则可以打开： [http://localhost:8563/arthas-output/](http://localhost:8563/arthas-output/) 查看到`arthas-output`目录下面的 JFR 记录结果：

![](/images/arthas-output-recording.png)

生成的结果可以用支持 jfr 格式的工具来查看。比如：

- JDK Mission Control ： https://github.com/openjdk/jmc
