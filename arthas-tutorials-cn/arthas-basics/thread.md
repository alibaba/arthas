`thread 1`{{execute interrupt}} 命令会打印线程 ID 1 的栈 - [thread 命令文档](https://arthas.aliyun.com/doc/thread.html)。

Arthas 支持管道，可以用 `thread 1 | grep 'main('`{{execute T2}} 查找到`main class`。

可以看到`main class`是`demo.MathGame`：

```
$ thread 1 | grep 'main('
    at demo.MathGame.main(MathGame.java:17)
```
