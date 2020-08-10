手动安装Arthas
===

## 手动安装Arthas


1. 下载最新版本

    **最新版本，点击下载**：[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/latest_version?mirror=aliyun)

2. 解压缩arthas的压缩包
    ```
    unzip arthas-packaging-bin.zip
    ```

3. 安装Arthas

    安装之前最好把所有老版本的Arthas全都删掉
    ```
    sudo su admin
    rm -rf /home/admin/.arthas/lib/*
    cd arthas
    ./install-local.sh
    ```
    > 注意，这里根据你需要诊断的Java进程的所属用户进行切换

4. 启动Arthas

    启动之前，请确保老版本的Arthas已经`stop`.

    ```
    ./as.sh
    ```

## 以脚本的方式启动as.sh/as.bat

### Linux/Unix/Mac

Arthas 支持在 Linux/Unix/Mac 等平台上一键安装，请复制以下内容，并粘贴到命令行中，敲 `回车` 执行即可：

```bash
curl -L https://arthas.aliyun.com/install.sh | sh
```

上述命令会下载启动脚本文件 `as.sh` 到当前目录，你可以放在任何地方或将其加入到 `$PATH` 中。

直接在shell下面执行`./as.sh`，就会进入交互界面。

也可以执行`./as.sh -h`来获取更多参数信息。


### Windows

最新版本，点击下载：[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/latest_version?mirror=aliyun)


下载解压后在bin目录有 `as.bat`。此脚本暂时只接受一个参数 pid，即只能诊断本机上的 Java 进程。（欢迎精通bat脚本的开发者改进）

```
as.bat <pid>
```

使用以下命令诊断windows服务模式运行的Java进程 (--interact 打开服务UI交互模式，方便诊断问题)：
```
as-service.bat -port <port>
as-service.bat -pid <pid>
as-service.bat -pid <pid> --interact
```

清理arthas windows服务执行以下命令：
```
as-service.bat -remove
```


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


