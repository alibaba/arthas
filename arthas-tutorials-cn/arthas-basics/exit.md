## 退出 Arthas

用 `exit`{{execute interrupt}} 或者 `quit`{{execute interrupt}} 命令可以退出 Arthas。

退出 Arthas 之后，还可以再次用 `java -jar arthas-boot.jar`{{execute interrupt}} 来连接。

## 彻底退出 Arthas

`exit/quit`命令只是退出当前 session，arthas server 还在目标进程中运行。

想完全退出 Arthas，可以执行 `stop`{{execute interrupt}} 命令。
