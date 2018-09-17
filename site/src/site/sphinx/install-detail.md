Arthas Install
=============


## Linux/Unix/Mac

Arthas 支持在 Linux/Unix/Mac 等平台上一键安装，请复制以下内容，并粘贴到命令行中，敲 `回车` 执行即可：

```bash
curl -L https://alibaba.github.io/arthas/install.sh | sh
```

上述命令会下载启动脚本文件 `as.sh` 到当前目录，你可以放在任何地方或将其加入到 `$PATH` 中。

直接在shell下面执行`./as.sh`，就会进入交互界面。

也可以执行`./as.sh -h`来获取更多参数信息。


## Windows

最新版本：[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22com.taobao.arthas%22%20AND%20a%3A%22arthas-packaging%22)


在`Download`栏下载最新的 `bin.zip` 包，解压后在bin目录有 `as.bat`。此脚本暂时只接受一个参数 pid，即只能诊断本机上的 Java 进程。（欢迎精通bat脚本的开发者改进）

```
as.bat pid
```

如果需要更好的体验，可以在本地使用 `as.bat pid` 启动 Arthas Server 后，然后在另外的 Linux/Mac 上使用 `as.sh pid@ip:port` 来远程诊断。


> Windows用户如果在cmd里不能正常显示颜色，可以使用[conemu](https://sourceforge.net/projects/conemu)。

## 手动安装

[手动安装](manual-install.md)

## 手动拼接命令行启动

如果启动遇到问题，可以尝试手动拼接出命令行参数来启动。

1. 查找目录jvm的java文件路径。

    在linux/mac上执行`ps aux | grep java`，在windows上可以通过进程管理器来查看。假设是`/opt/jdk1.8/bin/java`。

2. 拼接出命令行

    ```bash
    /opt/jdk1.8/bin/java -Xbootclasspath/a:/opt/jdk1.8/lib/tools.jar \
     -jar /tmp/arthas-packaging/arthas-core.jar \
     -pid 15146 \
     -target-ip 127.0.0.1 -telnet-port 3658 -http-port 8563 \
     -core /tmp/arthas-packaging/arthas-core.jar \
     -agent /tmp/arthas-packaging/arthas/arthas-agent.jar
    ```
    命令行分几部分组成：

    * `-Xbootclasspath` 增加tools.jar
    * `-jar /tmp/arthas-packaging/arthas-core.jar` 指定main函数入口
    * `-pid 15146` 指定目标java进程ID
    * `-target-ip 127.0.0.1` 指定IP
    * `-telnet-port 3658 -http-port 8563` 指定telnet和http端口
    * `-core /tmp/arthas-packaging/arthas-core.jar -agent /tmp/arthas-packaging/arthas/arthas-agent.jar` 指定core/agent jar包

    如果是`jdk > 9`，即9/10/11以上的版本，不需要指定`tools.jar`，直接去掉`-Xbootclasspath` 的配置即可。

    启动目志输出在`~/logs/arthas/arthas.log`里。
3. attach成功之后，使用telnet连接

    ```bash
    telnet localhost 3658
    ```


## 离线帮助文档

最新版本：[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22com.taobao.arthas%22%20AND%20a%3A%22arthas-packaging%22)

在`Download`栏下载最新的 `doc.zip` 包，


## 卸载

* 在 Linux/Unix/Mac 平台

    删除下面文件：
    ```bash
    rm -rf ~/.arthas/ ~/.arthas_history
    ```

* Windows平台直接删除zip包和解压的文件
