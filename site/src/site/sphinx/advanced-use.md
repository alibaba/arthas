进阶使用
===

## 基础命令


* help——查看命令帮助信息
* [cat](cat.md)——打印文件内容，和linux里的cat命令类似
* [echo](echo.md)--打印参数，和linux里的echo命令类似
* [grep](grep.md)——匹配查找，和linux里的grep命令类似
* [base64](base64.md)——base64编码转换，和linux里的base64命令类似
* [tee](tee.md)——复制标准输入到标准输出和指定的文件，和linux里的tee命令类似
* [pwd](pwd.md)——返回当前的工作目录，和linux命令类似
* cls——清空当前屏幕区域
* session——查看当前会话的信息
* [reset](reset.md)——重置增强类，将被 Arthas 增强过的类全部还原，Arthas 服务端关闭时会重置所有增强过的类
* version——输出当前目标 Java 进程所加载的 Arthas 版本号
* history——打印命令历史
* quit——退出当前 Arthas 客户端，其他 Arthas 客户端不受影响
* stop——关闭 Arthas 服务端，所有 Arthas 客户端全部退出
* [keymap](keymap.md)——Arthas快捷键列表及自定义快捷键

## jvm相关


* [dashboard](dashboard.md)——当前系统的实时数据面板
* [thread](thread.md)——查看当前 JVM 的线程堆栈信息
* [jvm](jvm.md)——查看当前 JVM 的信息
* [sysprop](sysprop.md)——查看和修改JVM的系统属性
* [sysenv](sysenv.md)——查看JVM的环境变量
* [vmoption](vmoption.md)——查看和修改JVM里诊断相关的option
* [perfcounter](perfcounter.md)——查看当前 JVM 的Perf Counter信息
* [logger](logger.md)——查看和修改logger
* [getstatic](getstatic.md)——查看类的静态属性
* [ognl](ognl.md)——执行ognl表达式
* [mbean](mbean.md)——查看 Mbean 的信息
* [heapdump](heapdump.md)——dump java heap, 类似jmap命令的heap dump功能
* [vmtool](vmtool.md)——从jvm里查询对象，执行forceGc
## class/classloader相关


* [sc](sc.md)——查看JVM已加载的类信息
* [sm](sm.md)——查看已加载类的方法信息
* [jad](jad.md)——反编译指定已加载类的源码
* [mc](mc.md)——内存编译器，内存编译`.java`文件为`.class`文件
* [retransform](retransform.md)——加载外部的`.class`文件，retransform到JVM里
* [redefine](redefine.md)——加载外部的`.class`文件，redefine到JVM里
* [dump](dump.md)——dump 已加载类的 byte code 到特定目录
* [classloader](classloader.md)——查看classloader的继承树，urls，类加载信息，使用classloader去getResource

## monitor/watch/trace相关


> 请注意，这些命令，都通过字节码增强技术来实现的，会在指定类的方法中插入一些切面来实现数据统计和观测，因此在线上、预发使用时，请尽量明确需要观测的类、方法以及条件，诊断结束要执行 `stop` 或将增强过的类执行 `reset` 命令。

* [monitor](monitor.md)——方法执行监控
* [watch](watch.md)——方法执行数据观测
* [trace](trace.md)——方法内部调用路径，并输出方法路径上的每个节点上耗时
* [stack](stack.md)——输出当前方法被调用的调用路径
* [tt](tt.md)——方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测


## profiler/火焰图

* [profiler](profiler.md)--使用[async-profiler](https://github.com/jvm-profiling-tools/async-profiler)对应用采样，生成火焰图

## 鉴权

* [auth](auth.md)--鉴权
## options

* [options](options.md)——查看或设置Arthas全局开关


## 管道

Arthas支持使用管道对上述命令的结果进行进一步的处理，如`sm java.lang.String * | grep 'index'`

* grep——搜索满足条件的结果
* plaintext——将命令的结果去除ANSI颜色
* wc——按行统计输出结果

## 后台异步任务

当线上出现偶发的问题，比如需要watch某个条件，而这个条件一天可能才会出现一次时，异步后台任务就派上用场了，详情请参考[这里](async.md)

* 使用 > 将结果重写向到日志文件，使用 & 指定命令是后台运行，session断开不影响任务执行（生命周期默认为1天）
* jobs——列出所有job
* kill——强制终止任务
* fg——将暂停的任务拉到前台执行
* bg——将暂停的任务放到后台执行

## Web Console

通过websocket连接Arthas。

* [Web Console](web-console.md)

## Arthas Properties

* [Arthas Properties](arthas-properties.md)

## 以java agent方式启动

* [以java agent方式启动](agent.md)


## as.sh 和 arthas-boot 技巧

* 通过`select`功能选择attach的进程。

正常情况下，每次执行`as.sh`/`arthas-boot.jar`需要选择，或者指定PID。这样会比较麻烦，因为每次启动应用，它的PID会变化。

比如，已经启动了`math-game.jar`，使用`jps`命令查看：

```bash
$ jps
58883 math-game.jar
58884 Jps
```

通过`select`参数可以指定进程名字，非常方便。

```bash
$ ./as.sh --select math-game
Arthas script version: 3.3.6
[INFO] JAVA_HOME: /tmp/java/8.0.222-zulu
Arthas home: /Users/admin/.arthas/lib/3.3.6/arthas
Calculating attach execution time...
Attaching to 59161 using version /Users/admin/.arthas/lib/3.3.6/arthas...

real	0m0.572s
user	0m0.281s
sys	0m0.039s
Attach success.
telnet connecting to arthas server... current timestamp is 1594280799
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'


wiki      https://arthas.aliyun.com/doc
tutorials https://arthas.aliyun.com/doc/arthas-tutorials.html
version   3.3.6
pid       58883
```


## 用户数据回报

在`3.1.4`版本后，增加了用户数据回报功能，方便统一做安全或者历史数据统计。

在启动时，指定`stat-url`，就会回报执行的每一行命令，比如： `./as.sh --stat-url 'http://192.168.10.11:8080/api/stat'`

在tunnel server里有一个示例的回报代码，用户可以自己在服务器上实现。

[StatController.java](https://github.com/alibaba/arthas/blob/master/tunnel-server/src/main/java/com/alibaba/arthas/tunnel/server/app/web/StatController.java)


## 其他特性

* [异步命令支持](async.md)
* [执行结果存日志](save-log.md)
* [批处理的支持](batch-support.md)
* [ognl表达式的用法说明](https://github.com/alibaba/arthas/issues/11)


