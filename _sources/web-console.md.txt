Web Console
===

[`Web Console`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=case-web-console)

### 通过浏览器连接arthas


Arthas目前支持Web Console，用户在attach成功之后，可以直接访问：[http://127.0.0.1:3658/](http://127.0.0.1:3658/)。

可以填入IP，远程连接其它机器上的arthas。

![](_static/web-console-local.png)


> 默认情况下，arthas只listen 127.0.0.1，所以如果想从远程连接，则可以使用 `--target-ip`参数指定listen的IP，更多参考`-h`的帮助说明。
> 注意会有安全风险，考虑下面的tunnel server的方案。

后续更多Web Console功能支持，请到issue下留言：[https://github.com/alibaba/arthas/issues/15](https://github.com/alibaba/arthas/issues/15)


### 使用arthas tunnel server连接远程arthas


#### 下载部署arthas tunnel server

[https://github.com/alibaba/arthas/releases](https://github.com/alibaba/arthas/releases)

Arthas tunnel server是一个spring boot fat jar应用，直接`java -jar`启动：

```bash
java -jar  arthas-tunnel-server.jar
```

默认情况下，arthas tunnel server的web端口是`8080`，arthas agent连接的端口是`7777`。

启动之后，可以访问 [http://127.0.0.1:8080/](http://127.0.0.1:8080/) ，再通过`agentId`连接到已注册的arthas agent上。

通过Spring Boot的Endpoint，可以查看到具体的连接信息： [http://127.0.0.1:8080/actuator/arthas](http://127.0.0.1:8080/actuator/arthas) ，登陆用户名是`arthas`，密码在arthas tunnel server的日志里可以找到，比如：

```
32851 [main] INFO  o.s.b.a.s.s.UserDetailsServiceAutoConfiguration

Using generated security password: f1dca050-3777-48f4-a577-6367e55a78a2
```

#### 启动arthas时连接到tunnel server

在启动arthas，可以传递`--tunnel-server`参数，比如：

```bash
as.sh --tunnel-server 'ws://127.0.0.1:7777/ws'
```

也可以使用下面的测试地址（不保证一直可用）：

```bash
as.sh --tunnel-server 'ws://47.75.156.201:7777/ws'
```

* 如果有特殊需求，可以通过`--agent-id`参数里指定agentId。默认情况下，会生成随机ID。

attach成功之后，会打印出agentId，比如：

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

如果是启动时没有连接到 tunnel server，也可以在后续自动重连成功之后，通过 session命令来获取 agentId：

```bash
[arthas@86183]$ session
 Name           Value
-----------------------------------------------------
 JAVA_PID       86183
 SESSION_ID     f7273eb5-e7b0-4a00-bc5b-3fe55d741882
 AGENT_ID       URJZ5L48RPBR2ALI5K4V
 TUNNEL_SERVER  ws://47.75.156.201:7777/ws
```


以上面的为例，在浏览器里访问 [http://47.75.156.201:8080/](http://47.75.156.201:8080/) ，输入 `agentId`，就可以连接到本机上的arthas了。


![](_static/arthas-tunnel-server.png)


#### Arthas tunnel server的工作原理

```
browser <-> arthas tunnel server <-> arthas tunnel client <-> arthas agent
```

[tunnel-server/README.md](https://github.com/alibaba/arthas/blob/master/tunnel-server/README.md#)