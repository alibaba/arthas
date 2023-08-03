手动新建一个终端于 `Tab 2`，在 `Tab 2` 里，下载`arthas-boot.jar`，再用`java -jar`命令启动：

`wget https://arthas.aliyun.com/arthas-boot.jar; java -jar arthas-boot.jar --target-ip 0.0.0.0`{{execute T2}}

`arthas-boot`是`Arthas`的启动程序，它启动后，会列出所有的 Java 进程，用户可以选择需要诊断的目标进程。

选择第一个进程，输入 `1`{{execute T2}} ，再`Enter/回车`：

Attach 成功之后，会打印 Arthas LOGO。输入 `help`{{execute T2}} 可以获取到更多的帮助信息。

![Arthas Boot](../../assets/arthas-boot.png)
