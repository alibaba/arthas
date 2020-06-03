

## reset

Arthas在 watch/trace 等命令时，实际上是修改了应用的字节码，插入增强的代码。显式执行 `reset`{{execute T2}} 命令，可以清除掉这些增强代码。

## 退出Arthas

用 `exit`{{execute interrupt}} 或者 `quit`{{execute interrupt}} 命令可以退出Arthas。

退出Arthas之后，还可以再次用 `java -jar arthas-boot.jar`{{execute interrupt}} 来连接。


## 彻底退出Arthas

`exit/quit`命令只是退出当前session，arthas server还在目标进程中运行。

想完全退出Arthas，可以执行 `stop`{{execute interrupt}} 命令。

