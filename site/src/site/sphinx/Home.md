Arthas 用户文档
===

## Arthas（阿尔萨斯） 能为你做什么？

当你遇到以下类似问题而束手无策时，你看到了这篇文档，看到了 `Arthas`，那么恭喜你，你朝正确的方向又迈了一大步。

0. 这个类从哪个 jar 包加载的？为什么会报各种类相关的 Exception？
0. 我改的代码为什么没有执行到？难道是我没 commit？分支搞错了？
0. 遇到问题无法在预发 debug 一下，难道只能通过加日志再重新预发布吗？
0. 线上遇到某个用户的数据处理有问题，但线上同样无法 debug，线下无法重现！
0. 是否有一个全局视角来查看系统的运行状况？
0. 有什么办法可以监控到容器和中间件（AliTomcat、HSF、Notify 等）的实时运行状态？

`Arthas` 是全新的在线诊断工具，采用命令行交互模式，支持web端在线诊断，同时提供丰富的 `Tab` 自动补全功能，进一步方便进行问题的定位和诊断。

## [Release Notes](release-notes)

## Arthas 安装及使用

### 1. 在线诊断

TODO

### 2. 通过脚本使用Arthas

* 下载脚本

#### MAC/Linux

```sh
curl -L TODO /install.sh | sh
```

#### Windows

点击 [下载](TODO) 最新zip包

> 如遇无法下载，请参考[这里](install-detail)

* 启动 Arthas

`./as.sh`

执行该脚本的用户需要和目标进程具有相同的权限。在公司内部通常需要使用admin用户执行。

`sudo su admin && ./as.sh` 或 `sudo -u admin -EH ./as.sh`
   
详细的启动脚本说明，请参考[这里](start-arthas)

如果attatch不上目标进程，可以查看`~/logs/arthas/` 目录下的日志。

### 3. 开启诊断之旅

为了使你快速找到适合你分析、诊断问题的命令，我们将 Arthas 的命令按问题的类型做了一个大的分类，如下：

#### 想快速了解系统、应用运行状况
---

* [dashboard](cmds/dashboard)——当前系统的实时数据面板
* [thread](cmds/thread)——查看当前 JVM 的线程堆栈信息
* [jvm](cmds/jvm)——查看当前 JVM 的信息
* [sysprop](cmds/sysprop)——查看和修改JVM的系统属性
* **New!** [getstatic](cmds/getstatic)——查看类的静态属性

#### 类、方法冲突、class文件、classloader继承问题等
---

* [sc](cmds/sc)——查看JVM已加载的类信息
* [sm](cmds/sm)——查看已加载类的方法信息
* [dump](cmds/dump)——dump 已加载类的 byte code 到特定目录
* [redefine](cmds/redefine)——加载外部的`.class`文件，redefine到JVM里
* [jad](cmds/jad)——反编译指定已加载类的源码
* [classloader](cmds/classloader)——查看classloader的继承树，urls，类加载信息，使用classloader去getResource

#### 查看方法执行参数、异常、返回值、调用路径等

> 非常重要，请注意，这些命令，都通过字节码增强技术来实现的，会在指定类的方法中插入一些切面来实现数据统计和观测，因此在线上、预发使用时，请尽量明确需要观测的类、方法以及条件，诊断结束要执行 `shutdown` 或将增强过的类执行 `reset` 命令。

* [monitor](cmds/monitor)——方法执行监控
* [watch](cmds/watch)——方法执行数据观测
* [trace](cmds/trace)——方法内部调用路径，并输出方法路径上的每个节点上耗时
* [stack](cmds/stack)——输出当前方法被调用的调用路径
* [tt](cmds/tt)——方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测

#### options

* [options](options)——查看或设置Arthas全局开关

#### Arthas 基础命令

* help——查看命令帮助信息
* cls——清空当前屏幕区域
* session——查看当前会话的信息
* [reset](cmds/reset)——重置增强类，将被 Arthas 增强过的类全部还原，Arthas 服务端关闭时会重置所有增强过的类
* version——输出当前目标 Java 进程所加载的 Arthas 版本号
* quit——退出当前 Arthas 客户端，其他 Arthas 客户端不受影响
* shutdown——关闭 Arthas 服务端，所有 Arthas 客户端全部退出
* [keymap](cmds/keymap)——Arthas快捷键列表及自定义快捷键

#### 管道

Arthas支持使用管道对上述命令的结果进行进一步的处理，如`sm org.apache.log4j.Logger | grep <init>`

* grep——搜索满足条件的结果
* plaintext——将命令的结果去除颜色
* wc——按行统计输出结果

#### 后台异步任务

当线上出现偶发的问题，比如需要watch某个条件，而这个条件一天可能才会出现一次时，异步后台任务就派上用场了，详情请参考[这里](async)

* 使用 > 将结果重写向到日志文件，使用 & 指定命令是后台运行，session断开不影响任务执行（生命周期默认为1天）
* jobs——列出所有job
* kill——强制终止任务
* fg——将暂停的任务拉到前台执行
* bg——将暂停的任务放到后台执行

### 4. 常见问题及FAQ

TODO

### 5. 其他特性说明

* [异步命令支持](async)
* [执行结果存日志](cmds/options)
* [批处理的支持](batch-support)
* [ognl表达式的用法说明](TODO/articles/92448)

### 6. 用户使用案例

* [用户使用案例](usercase)
* [Arthas——码农的才思与浪漫] TODO

钉钉交流群号：  TODO