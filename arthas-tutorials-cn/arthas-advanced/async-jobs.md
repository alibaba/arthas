arthas 中的后台异步任务，使用了仿 linux 系统任务相关的命令。[linux 任务相关介绍](https://ehlxr.me/2017/01/18/Linux-%E4%B8%AD-fg%E3%80%81bg%E3%80%81jobs%E3%80%81-%E6%8C%87%E4%BB%A4/)。

### 使用 & 在后台执行任务，任务输出重定向

可通过 `>` 或者 `>>` 将任务输出结果输出到指定的文件中，可以和 `&` 一起使用，实现 arthas 命令的后台异步任务。

当前我们需要排查一个问题，但是这个问题的出现时间不能确定，那我们就可以把检测命令挂在后台运行，并将保存到输出日志，如下命令：

`watch com.example.demo.arthas.user.UserController * '{params, throwExp}' 'throwExp != null' >> a.log &`{{exec}}

这时命令在后台执行，可以在 console 中继续执行其他命令。

之后我们去访问：[/user/0]({{TRAFFIC_HOST1_80}}/user/0)

然后使用 `cat a.log`{{exec}} 可以看到我们刚刚访问的 URL 抛出了一个异常

### 通过 jobs 查看任务

如果希望查看当前有哪些 arthas 任务在执行，可以执行 jobs 命令，执行结果如下

`jobs`{{execute T2}}

可以看到目前有一个后台任务在执行

job id 是 10, `*` 表示此 job 是当前 session 创建（生命周期默认为一天）

状态是 Stopped

`execution count` 是执行次数，从启动开始已经执行了 19 次

`timeout date` 是超时的时间，到这个时间，任务将会自动超时退出

### 停止命令、切换前后台

[异步执行的命令](https://arthas.fatpandac.com/doc/commands.html#%E5%90%8E%E5%8F%B0%E5%BC%82%E6%AD%A5%E4%BB%BB%E5%8A%A1)，如果希望停止，可执行 kill, 希望命令转到前台、后台继续执行 fg、bg 命令。

### 注意事项

- 最多同时支持 8 个命令使用重定向将结果写日志
- 请勿同时开启过多的后台异步命令，以免对目标 JVM 性能造成影响
- 如果不想停止 arthas，继续执行后台任务，可以执行 `quit` 退出 arthas 控制台（`stop` 会停止 arthas 服务）
