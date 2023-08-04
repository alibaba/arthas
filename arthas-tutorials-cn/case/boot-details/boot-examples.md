## 指定目标 pid

可以使用`jps`{{execute T2}}命令查看 pid。

直接在参数中添加 pid，可指定目标 pid。

`java -jar arthas-boot.jar 1`{{execute T2}}

## 允许外部访问

默认情况下，arthas server 侦听的是 `127.0.0.1` 这个 IP，如果希望远程可以访问，可以使用`--target-ip`的参数。

`java -jar arthas-boot.jar --target-ip 0.0.0.0`{{execute T2}}

## 指定侦听端口

默认情况下，arthas server 侦听的是 telnet 端口`3658`，http 端口`8563`，可分别使用`--telnet-port`，`--http-port`指定。

只侦听 Telnet 端口，不侦听 HTTP 端口：

`java -jar arthas-boot.jar --telnet-port 9999 --http-port -1`{{execute T2}}

## 指定 tunnel server

可以使用`--tunnel-server`参数指定。

`java -jar arthas-boot.jar --tunnel-server 'ws://192.168.10.11:7777/ws'`{{execute T2}}

如果 tunnel server 注册有 agent id，那么可以使用`--agent-id`参数指定。

`java -jar arthas-boot.jar --tunnel-server 'ws://192.168.10.11:7777/ws' --agent-id bvDOe8XbTM2pQWjF4cfw'`{{execute T2}}

## 指定报表统计地址

可以使用`--stat-url`参数指定。

`java -jar arthas-boot.jar --stat-url 'http://192.168.10.11:8080/api/stat'`{{execute T2}}

## 列出所有的版本

`java -jar arthas-boot.jar --versions`{{execute T2}}

使用指定版本：

`java -jar arthas-boot.jar --use-version 3.1.0`{{execute T2}}

## 打印运行的详情

使用`-v`或者`-verbose`。

`java -jar arthas-boot.jar -v`{{execute T2}}

## 指定需要执行的命令目标 pid

可以使用`--command`或者`-c`参数指定，并同时指定 pid，多个命令之间用`;`分隔。

`java -jar arthas-boot.jar -c 'sysprop; thread' 1`{{execute T2}}

## 指定需要执行的批处理文件目标 pid

可以使用`--command`或者`-c`参数指定，并同时指定 pid。

`java -jar arthas-boot.jar -f batch.as 1`{{execute T2}}

## 通过类名或者 jar 文件名指定目标进程

通过`--select`参数类名或者 jar 文件名指定目标进程

`java -jar arthas-boot.jar --select math-game`{{execute T2}}

## 指定会话超时秒数

使用`--session-timeout`参数指定，默认为 1800(30 分钟)

`java -jar arthas-boot.jar --session-timeout 3600`{{execute T2}}

## 仅附加目标进程，不连接

`java -jar arthas-boot.jar --attach-only`{{execute T2}}

## 指定镜像仓库，强制使用 http

`--repo-mirror`使用特定 maven 仓库镜像，参数可以为`center/aliyun`或 http 仓库地址。

`--use-http`强制使用 http 下载，默认使用 https。

`java -jar arthas-boot.jar --repo-mirror aliyun --use-http`{{execute T2}}

## 指定 arthas 客户端命令行宽高

`java -jar arthas-boot.jar --height 25 --width 80`{{execute T2}}

## 指定 arthas 主目录

`java -jar arthas-boot.jar --arthas-home .`{{execute T2}}

## 以 Java Agent 的方式启动

通常 Arthas 是以动态 attach 的方式来诊断应用，但从 3.2.0 版本起，Arthas 支持直接以 java agent 的方式启动。

比如下载全量的 arthas zip 包，解压之后以 -javaagent 的参数指定 arthas-agent.jar 来启动：

`java -javaagent:/tmp/test/arthas-agent.jar -jar math-game.jar`

默认的配置项在解压目录里的 arthas.properties 文件里。

参考：https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html
