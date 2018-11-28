Arthas Install
=============


## 全平台通用（推荐）

最新版本，点击下载：[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=bin&v=LATEST)

解压后，在文件夹里有`arthas-boot.jar`，直接用`java -jar`的方式启动：

```bash
java -jar arthas-boot.jar
```

打印帮助信息：

```bash
java -jar arthas-boot.jar -h
```


## Linux/Unix/Mac

Arthas 支持在 Linux/Unix/Mac 等平台上一键安装，请复制以下内容，并粘贴到命令行中，敲 `回车` 执行即可：

```bash
curl -L https://alibaba.github.io/arthas/install.sh | sh
```

上述命令会下载启动脚本文件 `as.sh` 到当前目录，你可以放在任何地方或将其加入到 `$PATH` 中。

直接在shell下面执行`./as.sh`，就会进入交互界面。

也可以执行`./as.sh -h`来获取更多参数信息。


## Windows

最新版本，点击下载：[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=bin&v=LATEST)


下载解压后在bin目录有 `as.bat`。此脚本暂时只接受一个参数 pid，即只能诊断本机上的 Java 进程。（欢迎精通bat脚本的开发者改进）

```
as.bat <pid>
```

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
    telnet 127.0.0.1 3658
    ```


## 离线帮助文档

最新版本，点击下载：[![Arthas](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.taobao.arthas&a=arthas-packaging&e=zip&c=doc&v=LATEST)


## 卸载

* 在 Linux/Unix/Mac 平台

    删除下面文件：
    ```bash
    rm -rf ~/.arthas/
    ```

* Windows平台直接删除zip包和解压的文件
