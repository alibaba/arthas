
可通过>或者>>将任务输出结果输出到指定的文件中，可以和&一起使用，实现arthas命令的后台异步任务。比如：

`trace demo.MathGame primeFactors >> test.out &`{{execute T2}}

这时trace命令会在后台执行，并且把结果输出到`~/logs/arthas-cache/test.out`。可继续执行其他命令。并可查看文件中的命令执行结果。

`cat test.out`{{execute T2}}

当连接到远程的arthas server时，可能无法查看远程机器的文件，arthas同时支持了自动重定向到本地缓存路径。使用方法如下：

`trace demo.MathGame primeFactors >> &`{{execute T2}}

```bash
$ trace Test t >>  &
job id  : 2
cache location  : /Users/gehui/logs/arthas-cache/28198/2
```

可以看到并没有指定重定向文件位置，arthas自动重定向到缓存中了，执行命令后会输出`job id`和`cache location`。`cache location`就是重定向文件的路径，在系统logs目录下，路径包括`pid`和`job id`，避免和其他任务冲突。命令输出结果到`/Users/gehui/logs/arthas-cache/28198/2`中，`job id`为2。
