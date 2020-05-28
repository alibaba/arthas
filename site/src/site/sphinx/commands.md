命令列表
=============

* [dashboard](dashboard.md)
* [thread](thread.md)
* [jvm](jvm.md)
* [sysprop](sysprop.md)
* [sysenv](sysenv.md)
* [vmoption](vmoption.md)
* [perfcounter](perfcounter.md)
* [logger](logger.md)
* [mbean](mbean.md)
* [getstatic](getstatic.md)

* [ognl](ognl.md)

* [sc](sc.md)
* [sm](sm.md)
* [dump](dump.md)
* [heapdump](heapdump.md)

* [jad](jad.md)
* [classloader](classloader.md)
* [mc](mc.md)
* [redefine](redefine.md)

* [monitor](monitor.md)
* [watch](watch.md)
* [trace](trace.md)
* [stack](stack.md)
* [tt](tt.md)

* [profiler](profiler.md)

* [cat](cat.md)
* [echo](echo.md)
* [grep](grep.md)
* [tee](tee.md)
* [pwd](pwd.md)
* [options](options.md)

### Arthas 基础命令

* help——查看命令帮助信息
* cls——清空当前屏幕区域
* session——查看当前会话的信息
* [reset](reset.md)——重置增强类，将被 Arthas 增强过的类全部还原，Arthas 服务端关闭时会重置所有增强过的类
* version——输出当前目标 Java 进程所加载的 Arthas 版本号
* history——打印命令历史
* quit——退出当前 Arthas 客户端，其他 Arthas 客户端不受影响
* stop——关闭 Arthas 服务端，所有 Arthas 客户端全部退出
* [keymap](keymap.md)——Arthas快捷键列表及自定义快捷键