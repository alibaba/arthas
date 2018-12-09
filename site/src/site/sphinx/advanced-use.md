进阶使用
===

## 基础命令


* help——查看命令帮助信息
* cls——清空当前屏幕区域
* session——查看当前会话的信息
* [reset](reset.md)——重置增强类，将被 Arthas 增强过的类全部还原，Arthas 服务端关闭时会重置所有增强过的类
* version——输出当前目标 Java 进程所加载的 Arthas 版本号
* history——打印命令历史
* quit——退出当前 Arthas 客户端，其他 Arthas 客户端不受影响
* shutdown——关闭 Arthas 服务端，所有 Arthas 客户端全部退出
* [keymap](keymap.md)——Arthas快捷键列表及自定义快捷键

## jvm相关


* [dashboard](dashboard.md)——当前系统的实时数据面板
* [thread](thread.md)——查看当前 JVM 的线程堆栈信息
* [jvm](jvm.md)——查看当前 JVM 的信息
* [sysprop](sysprop.md)——查看和修改JVM的系统属性
* [sysenv](sysenv.md)——查看JVM的环境变量
* [getstatic](getstatic.md)——查看类的静态属性
* **New!** [ognl](ognl.md)——执行ognl表达式

## class/classloader相关


* [sc](sc.md)——查看JVM已加载的类信息
* [sm](sm.md)——查看已加载类的方法信息
* [dump](dump.md)——dump 已加载类的 byte code 到特定目录
* [redefine](redefine.md)——加载外部的`.class`文件，redefine到JVM里
* [jad](jad.md)——反编译指定已加载类的源码
* [classloader](classloader.md)——查看classloader的继承树，urls，类加载信息，使用classloader去getResource

## monitor/watch/trace相关


> 请注意，这些命令，都通过字节码增强技术来实现的，会在指定类的方法中插入一些切面来实现数据统计和观测，因此在线上、预发使用时，请尽量明确需要观测的类、方法以及条件，诊断结束要执行 `shutdown` 或将增强过的类执行 `reset` 命令。

* [monitor](monitor.md)——方法执行监控
* [watch](watch.md)——方法执行数据观测
* [trace](trace.md)——方法内部调用路径，并输出方法路径上的每个节点上耗时
* [stack](stack.md)——输出当前方法被调用的调用路径
* [tt](tt.md)——方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测

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

## 其他特性

* [异步命令支持](async.md)
* [执行结果存日志](save-log.md)
* [批处理的支持](batch-support.md)
* [ognl表达式的用法说明](https://github.com/alibaba/arthas/issues/11)


