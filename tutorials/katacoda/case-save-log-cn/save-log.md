

将命令的结果完整保存在日志文件中，便于后续进行分析

- 默认情况下，该功能是关闭的，如果需要开启，请执行以下命令：

`options save-result true`{{execute T2}}

```bash
$ options save-result true
 NAME         BEFORE-VALUE  AFTER-VALUE
----------------------------------------
 save-result  false         true
Affect(row-cnt:1) cost in 3 ms.
```

看到上面的输出，即表示成功开启该功能；

- 日志文件路径

结果会异步保存在：`{user.home}/logs/arthas-cache/result.log`，请定期进行清理，以免占据磁盘空间

## 使用Arthas的异步后台任务将结果存日志文件

```bash
$ trace Test t >>  &
job id  : 2
cache location  : /Users/admin/logs/arthas-cache/28198/2
```

* 参考： https://arthas.aliyun.com/doc/async.html

