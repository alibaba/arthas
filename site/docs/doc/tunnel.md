# Arthas Tunnel

通过 Arthas Tunnel Server/Client 来远程管理/连接多个 Agent。

比如，在流式计算里，Java 进程可以是在不同的机器启动的，想要使用 Arthas 去诊断会比较麻烦，因为用户通常没有机器的权限，即使登陆机器也分不清是哪个 Java 进程。

在这种情况下，可以使用 Arthas Tunnel Server/Client。

参考:

- 1: [Web Console](web-console.md)
- 2: [Arthas Spring Boot Starter](spring-boot-starter.md)

## 下载部署 arthas tunnel server

[https://github.com/alibaba/arthas/releases](https://github.com/alibaba/arthas/releases)

- 从 Maven 仓库下载：[![](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg?style=flat-square "Arthas")](https://arthas.aliyun.com/download/arthas-tunnel-server/latest_version?mirror=aliyun)

- 从 Github Releases 页下载： [https://github.com/alibaba/arthas/releases](https://github.com/alibaba/arthas/releases)

Arthas tunnel server 是一个 spring boot fat jar 应用，直接`java -jar`启动：

```bash
java -jar  arthas-tunnel-server.jar
```

默认情况下，arthas tunnel server 的 web 端口是`8080`，arthas agent 连接的端口是`7777`。

启动之后，可以访问 [http://127.0.0.1:8080/](http://127.0.0.1:8080/) ，再通过`agentId`连接到已注册的 arthas agent 上。

通过 Spring Boot 的 Endpoint，可以查看到具体的连接信息： [http://127.0.0.1:8080/actuator/arthas](http://127.0.0.1:8080/actuator/arthas) ，登陆用户名是`arthas`，密码在 arthas tunnel server 的日志里可以找到，比如：

```
32851 [main] INFO  o.s.b.a.s.s.UserDetailsServiceAutoConfiguration

Using generated security password: f1dca050-3777-48f4-a577-6367e55a78a2
```

## 启动 arthas 时连接到 tunnel server

在启动 arthas，可以传递`--tunnel-server`参数，比如：

```bash
as.sh --tunnel-server 'ws://127.0.0.1:7777/ws'
```

也可以使用下面的测试地址（不保证一直可用）：

```bash
as.sh --tunnel-server 'ws://47.75.156.201:80/ws'
```

- 如果有特殊需求，可以通过`--agent-id`参数里指定 agentId。默认情况下，会生成随机 ID。

attach 成功之后，会打印出 agentId，比如：

```bash
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'


wiki      https://arthas.aliyun.com/doc
tutorials https://arthas.aliyun.com/doc/arthas-tutorials.html
version   3.1.2
pid       86183
time      2019-08-30 15:40:53
id        URJZ5L48RPBR2ALI5K4V
```

如果是启动时没有连接到 tunnel server，也可以在后续自动重连成功之后，通过 session 命令来获取 agentId：

```bash
[arthas@86183]$ session
 Name           Value
-----------------------------------------------------
 JAVA_PID       86183
 SESSION_ID     f7273eb5-e7b0-4a00-bc5b-3fe55d741882
 AGENT_ID       URJZ5L48RPBR2ALI5K4V
 TUNNEL_SERVER  ws://47.75.156.201:80/ws
```

以上面的为例，在浏览器里访问 [http://47.75.156.201/arthas/?port=80](http://47.75.156.201/arthas/?port=80) ，输入 `agentId`，就可以连接到本机上的 arthas 了。

![](/images/arthas-tunnel-server.png)

## 最佳实践

::: tip
注意，agentId 要保持唯一，否则会在 tunnel server 上冲突，不能正常工作。
:::

如果 arthas agent 配置了 `appName`，则生成的 agentId 会带上`appName`的前缀。

比如在加上启动参数：`as.sh --tunnel-server 'ws://127.0.0.1:7777/ws' --app-name demoapp` ，则生成的 agentId 可能是`demoapp_URJZ5L48RPBR2ALI5K4V`。

Tunnel server 会以`_`做分隔符，提取出`appName`，方便按应用进行管理。

::: tip
另外，也可以在解压的 arthas 目录下的 `arthas.properties`，或者在 spring boot 应用的`application.properties`里配置`appName`。
:::

## Tunnel Server 的管理页面

::: tip
需要在 tunnel-server 的`application.properties`里配置 `arthas.enable-detail-pages=true`，也可以用命令行参数指定： `java -Darthas.enable-detail-pages=true -jar arthas-tunnel-server.jar`

支持的配置项： [tunnel-server application.properties](https://github.com/alibaba/arthas/blob/master/tunnel-server/src/main/resources/application.properties)

**注意，开放管理页面有风险！管理页面没有安全拦截功能，务必自行增加安全措施，不要开放到公网。**
:::

在本地启动 tunnel-server，然后使用`as.sh` attach，并且指定应用名`--app-name test`：

```
$ as.sh --tunnel-server 'ws://127.0.0.1:7777/ws' --app-name test
telnet connecting to arthas server... current timestamp is 1627539688
Trying 127.0.0.1...
Connected to 127.0.0.1.
Escape character is '^]'.
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'


wiki       https://arthas.aliyun.com/doc
tutorials  https://arthas.aliyun.com/doc/arthas-tutorials.html
version    3.5.3
main_class demo.MathGame
pid        65825
time       2021-07-29 14:21:29
id         test_PE3LZO9NA9ENJYTPGL9L
```

然后访问 tunnel-server，可以看到所有连接的应用列表：

[http://localhost:8080/apps.html](http://localhost:8080/apps.html)

![](/images/tunnel-server-apps.png)

再打开详情，则可以看到连接的所有 agent 列表：

[http://localhost:8080/agents.html?app=test](http://localhost:8080/agents.html?app=test)

![](/images/tunnel-server-agents.png)

## 安全和权限管理

::: tip
**强烈建议不要把 tunnel server 直接暴露到公网上。**
:::

目前 tunnel server 没有专门的权限管理

1. 用户需要自行开发，对 app name 鉴权。
2. 如果开放管理页面，需要增加安全措施。

## 集群方式管理

如果希望部署多台 tunnel server，可以通过 nginx 做转发，redis 来保存 agent 信息。

- nginx 需要配置 sticky session，保证用户 web socket 连接到同一个后端 tunnel server 上。简单的配置方式是用`ip_hash`。

## Arthas tunnel server 的工作原理

```
browser <-> arthas tunnel server <-> arthas tunnel client <-> arthas agent
```

[tunnel-server/README.md](https://github.com/alibaba/arthas/blob/master/tunnel-server/README.md#)
