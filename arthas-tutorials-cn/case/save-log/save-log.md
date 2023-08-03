将命令的结果完整保存在日志文件中，便于后续进行分析

- 默认情况下，该功能是关闭的，如果需要开启，请执行以下命令：

`options save-result true`{{exec}}

正常输出，即表示成功开启该功能；

- 日志文件路径

结果会异步保存在：`{user.home}/logs/arthas-cache/result.log`，请定期进行清理，以免占据磁盘空间

## 使用 Arthas 的异步后台任务将结果存日志文件

运行 `trace demo.MathGame run >> a.log &`{{exec}}，这时候任务已经在后台挂起，使用 `jobs`{{exec}} 查看当前后台运行的任务，之后可以使用 kill 指定任务 ID 将其结束。

使用 `cat a.log`{{exec}} 可以看到运行命令的输出已经重定向到了 `a.log` 文件中。

- 参考：https://arthas.aliyun.com/doc/async.html
