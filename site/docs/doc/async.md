# Arthas 后台异步任务

[`后台异步任务`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=case-async-jobs)

arthas 中的后台异步任务，使用了仿 linux 系统任务相关的命令。[linux 任务相关介绍](https://ehlxr.me/2017/01/18/Linux-%E4%B8%AD-fg%E3%80%81bg%E3%80%81jobs%E3%80%81-%E6%8C%87%E4%BB%A4/)。

## 1. 使用&在后台执行任务

比如希望执行后台执行 trace 命令，那么调用下面命令

```bash
trace Test t &
```

这时命令在后台执行，可以在 console 中继续执行其他命令。

## 2. 通过 jobs 查看任务

如果希望查看当前有哪些 arthas 任务在执行，可以执行 jobs 命令，执行结果如下

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

- job id 是 10, `*` 表示此 job 是当前 session 创建
- 状态是 Stopped
- execution count 是执行次数，从启动开始已经执行了 19 次
- timeout date 是超时的时间，到这个时间，任务将会自动超时退出

## 3. 任务暂停和取消

当任务正在前台执行，比如直接调用命令`trace Test t`或者调用后台执行命令`trace Test t &`后又通过`fg`命令将任务转到前台。这时 console 中无法继续执行命令，但是可以接收并处理以下事件：

- ‘ctrl + z’：将任务暂停。通过`jobs`查看任务状态将会变为 Stopped，通过`bg <job-id>`或者`fg <job-id>`可让任务重新开始执行
- ‘ctrl + c’：停止任务
- ‘ctrl + d’：按照 linux 语义应当是退出终端，目前 arthas 中是空实现，不处理

## 4. fg、bg 命令，将命令转到前台、后台继续执行

- 任务在后台执行或者暂停状态（`ctrl + z`暂停任务）时，执行`fg <job-id>`将可以把对应的任务转到前台继续执行。在前台执行时，无法在 console 中执行其他命令
- 当任务处于暂停状态时（`ctrl + z`暂停任务），执行`bg <job-id>`将可以把对应的任务在后台继续执行
- 非当前 session 创建的 job，只能由当前 session fg 到前台执行

## 5. 任务输出重定向

可通过`>`或者`>>`将任务输出结果输出到指定的文件中，可以和`&`一起使用，实现 arthas 命令的后台异步任务。比如：

```bash
$ trace Test t >> test.out &
```

这时 trace 命令会在后台执行，并且把结果输出到应用`工作目录`下面的`test.out`文件。可继续执行其他命令。并可查看文件中的命令执行结果。可以执行`pwd`命令查看当前应用的`工作目录`。

```bash
$ cat test.out
```

如果没有指定重定向文件，则会把结果输出到`~/logs/arthas-cache/`目录下，比如：

```bash
$ trace Test t >>  &
job id  : 2
cache location  : /Users/admin/logs/arthas-cache/28198/2
```

此时命令会在后台异步执行，并将结果异步保存在文件（`~/logs/arthas-cache/${PID}/${JobId}`）中；

- 此时任务的执行不受 session 断开的影响；任务默认超时时间是 1 天，可以通过全局 `options` 命令修改默认超时时间；
- 此命令的结果将异步输出到  文件中；此时不管 `save-result` 是否为 true，都不会再往`~/logs/arthas-cache/result.log` 中异步写结果。

## 6. 停止命令

异步执行的命令，如果希望停止，可执行`kill <job-id>`

## 7. 其他

- 最多同时支持 8 个命令使用重定向将结果写日志
- 请勿同时开启过多的后台异步命令，以免对目标 JVM 性能造成影响
- 如果不想停止 arthas，继续执行后台任务，可以执行 `quit` 退出 arthas 控制台（`stop` 会停止 arthas 服务）
