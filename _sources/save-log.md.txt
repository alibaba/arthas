执行结果存日志
===

[`执行结果存日志`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=case-save-log)

> 将命令的结果完整保存在日志文件中，便于后续进行分析

* 默认情况下，该功能是关闭的，如果需要开启，请执行以下命令：

```bash
$ options save-result true
 NAME         BEFORE-VALUE  AFTER-VALUE
----------------------------------------
 save-result  false         true
Affect(row-cnt:1) cost in 3 ms.
```

看到上面的输出，即表示成功开启该功能；

* 日志文件路径

结果会异步保存在：`{user.home}/logs/arthas-cache/result.log`，请定期进行清理，以免占据磁盘空间

### 使用新版本Arthas的异步后台任务将结果存日志文件

```bash
$ trace Test t >>  &
job id  : 2
cache location  : /Users/admin/logs/arthas-cache/28198/2
```

此时命令会在后台异步执行，并将结果异步保存在文件（~/logs/arthas-cache/${PID}/${JobId}）中；

* 此时任务的执行不受session断开的影响；任务默认超时时间是1天，可以通过全局 `options` 命令修改默认超时时间；
* 此命令的结果将异步输出到文件中；此时不管 `save-result` 是否为true，都不会再往~/logs/arthas-cache/result.log 中异步写结果