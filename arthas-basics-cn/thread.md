`thread 1`{{execute interrupt}} 命令会打印线程ID 1的栈。


Arthas支持管道，可以用 `thread 1 | grep 'main('`{{execute T2}} 查找到`main class`。

可以看到`main class`是`demo.MathGame`：

```
$ thread 1 | grep 'main('
    at demo.MathGame.main(MathGame.java:17)
```
