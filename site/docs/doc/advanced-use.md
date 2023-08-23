# 其他特性

## Arthas 后台异步任务

当需要排查一个问题，但是这个问题的出现时间不能确定，那我们就可以把检测命令挂在后台运行，并将保存到输出日志。

- [Arthas 后台异步任务](async.md)

## 执行结果存日志

所有执行记录的结果完整保存在日志文件中，便于后续进行分析。

- [执行结果存日志](save-log.md)

## Docker

Arthas 在 docker 容器中使用配置参考。

- [Docker](docker.md)

## Web Console

通过 websocket 连接 Arthas。

- [Web Console](web-console.md)

## Arthas Tunnel

通过 Arthas Tunnel Server/Client 来远程管理/连接多个服务器下的Java服务。

- [Arthas Tunnel](tunnel.md)

## ognl 表达式用法

- [ognl 表达式的用法说明](https://github.com/alibaba/arthas/issues/11)
- [一些 ognl 特殊用法](https://github.com/alibaba/arthas/issues/71)

## IDEA Plugin

IntelliJ IDEA 编译器中更加快捷构建 arhtas 命令。

- [IDEA Plugin](idea-plugin.md)

## Arthas Properties

Arthas 支持配置项参考。

- [Arthas Properties](arthas-properties.md)

## 以 java agent 方式启动

- [以 java agent 方式启动](agent.md)

## Arthas Spring Boot Starter

随应用一起启动。

- [Arthas Spring Boot Starter](spring-boot-starter.md)

## HTTP API

Http API 提供结构化的数据，支持更复杂的交互功能，方便自定义界面集成 arthas。

- [HTTP API](http-api.md)

## 批处理功能

方便自定义脚本一次性批量运行多个命令，可结合 `--select` 参数可以指定进程名字一起使用。

- [批处理功能](batch-support.md)

## as.sh 和 arthas-boot 技巧

- 通过`select`功能选择 attach 的进程。

正常情况下，每次执行`as.sh`/`arthas-boot.jar`需要选择，或者指定 PID。这样会比较麻烦，因为每次启动应用，它的 PID 会变化。

比如，已经启动了`math-game.jar`，使用`jps`命令查看：

```bash
$ jps
58883 math-game.jar
58884 Jps
```

通过`select`参数可以指定进程名字，非常方便。

```bash
$ ./as.sh --select math-game
Arthas script version: 3.3.6
[INFO] JAVA_HOME: /tmp/java/8.0.222-zulu
Arthas home: /Users/admin/.arthas/lib/3.3.6/arthas
Calculating attach execution time...
Attaching to 59161 using version /Users/admin/.arthas/lib/3.3.6/arthas...

real	0m0.572s
user	0m0.281s
sys	0m0.039s
Attach success.
telnet connecting to arthas server... current timestamp is 1594280799
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'


wiki      https://arthas.aliyun.com/doc
tutorials https://arthas.aliyun.com/doc/arthas-tutorials.html
version   3.3.6
pid       58883
```

## 用户数据回报

在`3.1.4`版本后，增加了用户数据回报功能，方便统一做安全或者历史数据统计。

在启动时，指定`stat-url`，就会回报执行的每一行命令，比如： `./as.sh --stat-url 'http://192.168.10.11:8080/api/stat'`

在 tunnel server 里有一个示例的回报代码，用户可以自己在服务器上实现。

[StatController.java](https://github.com/alibaba/arthas/blob/master/tunnel-server/src/main/java/com/alibaba/arthas/tunnel/server/app/web/StatController.java)
