# 命令列表

## jvm 相关

- [dashboard](dashboard.md) - 当前系统的实时数据面板
- [getstatic](getstatic.md) - 查看类的静态属性
- [heapdump](heapdump.md) - dump java heap, 类似 jmap 命令的 heap dump 功能
- [jvm](jvm.md) - 查看当前 JVM 的信息
- [logger](logger.md) - 查看和修改 logger
- [mbean](mbean.md) - 查看 Mbean 的信息
- [memory](memory.md) - 查看 JVM 的内存信息
- [ognl](ognl.md) - 执行 ognl 表达式
- [perfcounter](perfcounter.md) - 查看当前 JVM 的 Perf Counter 信息
- [sysenv](sysenv.md) - 查看 JVM 的环境变量
- [sysprop](sysprop.md) - 查看和修改 JVM 的系统属性
- [thread](thread.md) - 查看当前 JVM 的线程堆栈信息
- [vmoption](vmoption.md) - 查看和修改 JVM 里诊断相关的 option
- [vmtool](vmtool.md) - 从 jvm 里查询对象，执行 forceGc

## class/classloader 相关

- [classloader](classloader.md) - 查看 classloader 的继承树，urls，类加载信息，使用 classloader 去 getResource
- [dump](dump.md) - dump 已加载类的 byte code 到特定目录
- [jad](jad.md) - 反编译指定已加载类的源码
- [mc](mc.md) - 内存编译器，内存编译`.java`文件为`.class`文件
- [redefine](redefine.md) - 加载外部的`.class`文件，redefine 到 JVM 里
- [retransform](retransform.md) - 加载外部的`.class`文件，retransform 到 JVM 里
- [sc](sc.md) - 查看 JVM 已加载的类信息
- [sm](sm.md) - 查看已加载类的方法信息

## monitor/watch/trace 相关

::: warning
请注意，这些命令，都通过字节码增强技术来实现的，会在指定类的方法中插入一些切面来实现数据统计和观测，因此在线上、预发使用时，请尽量明确需要观测的类、方法以及条件，诊断结束要执行 `stop` 或将增强过的类执行 `reset` 命令。
:::

- [monitor](monitor.md) - 方法执行监控
- [stack](stack.md) - 输出当前方法被调用的调用路径
- [trace](trace.md) - 方法内部调用路径，并输出方法路径上的每个节点上耗时
- [tt](tt.md) - 方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测
- [watch](watch.md) - 方法执行数据观测

## profiler/火焰图

- [profiler](profiler.md) - 使用[async-profiler](https://github.com/jvm-profiling-tools/async-profiler)对应用采样，生成火焰图
- [jfr](jfr.md) - 动态开启关闭 JFR 记录

## 鉴权

- [auth](auth.md) - 鉴权

## options

- [options](options.md) - 查看或设置 Arthas 全局开关

## 管道

Arthas 支持使用管道对上述命令的结果进行进一步的处理，如`sm java.lang.String * | grep 'index'`

- [grep](grep.md) - 搜索满足条件的结果
- plaintext - 将命令的结果去除 ANSI 颜色
- wc - 按行统计输出结果

## 后台异步任务

当线上出现偶发的问题，比如需要 watch 某个条件，而这个条件一天可能才会出现一次时，异步后台任务就派上用场了，详情请参考[这里](async.md)

- 使用 `>` 将结果重写向到日志文件，使用 `&` 指定命令是后台运行，session 断开不影响任务执行（生命周期默认为 1 天）
- jobs - 列出所有 job
- kill - 强制终止任务
- fg - 将暂停的任务拉到前台执行
- bg - 将暂停的任务放到后台执行

## 基础命令

- [base64](base64.md) - base64 编码转换，和 linux 里的 base64 命令类似
- [cat](cat.md) - 打印文件内容，和 linux 里的 cat 命令类似
- [cls](cls.md) - 清空当前屏幕区域
- [echo](echo.md) - 打印参数，和 linux 里的 echo 命令类似
- [grep](grep.md) - 匹配查找，和 linux 里的 grep 命令类似
- [help](help.md) - 查看命令帮助信息
- [history](history.md) - 打印命令历史
- [keymap](keymap.md) - Arthas 快捷键列表及自定义快捷键
- [pwd](pwd.md) - 返回当前的工作目录，和 linux 命令类似
- [quit](quit.md) - 退出当前 Arthas 客户端，其他 Arthas 客户端不受影响
- [reset](reset.md) - 重置增强类，将被 Arthas 增强过的类全部还原，Arthas 服务端关闭时会重置所有增强过的类
- [session](session.md) - 查看当前会话的信息
- [stop](stop.md) - 关闭 Arthas 服务端，所有 Arthas 客户端全部退出
- [tee](tee.md) - 复制标准输入到标准输出和指定的文件，和 linux 里的 tee 命令类似
- [version](version.md) - 输出当前目标 Java 进程所加载的 Arthas 版本号
