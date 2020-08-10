Arthas后台异步任务
===

[`后台异步任务`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=case-async-jobs)

arthas中的后台异步任务，使用了仿linux系统任务相关的命令。[linux任务相关介绍](https://ehlxr.me/2017/01/18/Linux-%E4%B8%AD-fg%E3%80%81bg%E3%80%81jobs%E3%80%81-%E6%8C%87%E4%BB%A4/)。


## 1. 使用&在后台执行任务
比如希望执行后台执行trace命令，那么调用下面命令

```bash
trace Test t &  
```
这时命令在后台执行，可以在console中继续执行其他命令。

## 2. 通过jobs查看任务
如果希望查看当前有哪些arthas任务在执行，可以执行jobs命令，执行结果如下
```bash
$ jobs
[10]*
       Stopped           watch com.taobao.container.Test test "params[0].{? #this.name == null }" -x 2
       execution count : 19
       start time      : Fri Sep 22 09:59:55 CST 2017
       timeout date    : Sat Sep 23 09:59:55 CST 2017
       session         : 3648e874-5e69-473f-9eed-7f89660b079b (current)
```
可以看到目前有一个后台任务在执行。
* job id是10, `*` 表示此job是当前session创建
* 状态是Stopped
* execution count是执行次数，从启动开始已经执行了19次
* timeout date是超时的时间，到这个时间，任务将会自动超时退出

## 3. 任务暂停和取消
当任务正在前台执行，比如直接调用命令`trace Test t`或者调用后台执行命令`trace Test t &`后又通过`fg`命令将任务转到前台。这时console中无法继续执行命令，但是可以接收并处理以下事件：

* ‘ctrl + z’：将任务暂停。通过`jbos`查看任务状态将会变为Stopped，通过`bg <job-id>`或者`fg <job-id>`可让任务重新开始执行
* ‘ctrl + c’：停止任务
* ‘ctrl + d’：按照linux语义应当是退出终端，目前arthas中是空实现，不处理

## 4. fg、bg命令，将命令转到前台、后台继续执行
* 任务在后台执行或者暂停状态（`ctrl + z`暂停任务）时，执行`fg <job-id>`将可以把对应的任务转到前台继续执行。在前台执行时，无法在console中执行其他命令
* 当任务处于暂停状态时（`ctrl + z`暂停任务），执行`bg <job-id>`将可以把对应的任务在后台继续执行
* 非当前session创建的job，只能由当前session fg到前台执行

## 5. 任务输出重定向
可通过`>`或者`>>`将任务输出结果输出到指定的文件中，可以和`&`一起使用，实现arthas命令的后台异步任务。比如：

```bash
$ trace Test t >> test.out &
```
这时trace命令会在后台执行，并且把结果输出到~/logs/arthas-cache/test.out。可继续执行其他命令。并可查看文件中的命令执行结果。

当连接到远程的arthas server时，可能无法查看远程机器的文件，arthas同时支持了自动重定向到本地缓存路径。使用方法如下：
```bash
$ trace Test t >>  &
job id  : 2
cache location  : /Users/gehui/logs/arthas-cache/28198/2
```
可以看到并没有指定重定向文件位置，arthas自动重定向到缓存中了，执行命令后会输出job id和cache location。cache location就是重定向文件的路径，在系统logs目录下，路径包括pid和job id，避免和其他任务冲突。命令输出结果到`/Users/gehui/logs/arthas-cache/28198/2`中，job id为2。

## 6. 停止命令
异步执行的命令，如果希望停止，可执行kill <job-id>

## 7. 其他

* 最多同时支持8个命令使用重定向将结果写日志
* 请勿同时开启过多的后台异步命令，以免对目标JVM性能造成影响
* 如果不想停止arthas，继续执行后台任务，可以执行 `quit` 退出arthas控制台（`stop` 会停止arthas 服务）
