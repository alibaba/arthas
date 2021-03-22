Arthas Tunnel
===


Manage/connect multiple Agents remotely via Arthas Tunnel Server/Client.

For example, in streaming computing, Java processes can be started on different machines, and it can be difficult to use Arthas to diagnose them, because the user usually does not have access to the machine.

In this case, Arthas Tunnel Server/Client can be used.

Reference: 
* [Web Console](web-console.md)
* [Arthas Spring Boot Starter](spring-boot-starter.md)

### Download and deploy arthas tunnel server

[https://github.com/alibaba/arthas/releases](https://github.com/alibaba/arthas/releases)

Arthas tunnel server is a spring boot fat jar application, start with the `java -jar` command:

```bash
java -jar  arthas-tunnel-server.jar
```

By default, the web port of the arthas tunnel server is `8080`, and the port connected by the arthas agent is `7777`.

Once started, you can go to [http://127.0.0.1:8080/](http://127.0.0.1:8080/) and connect to the registered arthas agent via `agentId`.

Through Spring Boot's Endpoint, you can view the specific connection information: [http://127.0.0.1:8080/actuator/arthas](http://127.0.0.1:8080/actuator/arthas), the login user name is `arthas`, and the password can be found in the log of arthas tunnel server, for example:

```
32851 [main] INFO o.s.b.a.s.s.UserDetailsServiceAutoConfiguration

Using generated security password: f1dca050-3777-48f4-a577-6367e55a78a2
```

### Connecting to the tunnel server when starting arthas


When starting arthas, you can use the `--tunnel-server` parameter, for example:

```bash
as.sh --tunnel-server 'ws://127.0.0.1:7777/ws'
```

You can also use the following test address (not guaranteed to be available all the time):

```bash
as.sh --tunnel-server 'ws://47.75.156.201:80/ws'
```

* You can specify the agentId by the `--agent-id` parameter. By default, a random ID is generated.

After Arthas attach succeeds, the agentId will be printed, such as:

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

If the connection is not connected to the tunnel server at startup, you can also obtain the agentId through the `session` command after reconnection succeeds:

```bash
[arthas@86183]$ session
 Name           Value
-----------------------------------------------------
 JAVA_PID       86183
 SESSION_ID     f7273eb5-e7b0-4a00-bc5b-3fe55d741882
 AGENT_ID       URJZ5L48RPBR2ALI5K4V
 TUNNEL_SERVER  ws://47.75.156.201:80/ws
```


For the above example, go to [http://47.75.156.201/arthas/?port=80](http://47.75.156.201/arthas/?port=80) in the browser and input the `agentId` to connect to arthas on remote machine.


![](_static/arthas-tunnel-server.png)


### Best practices

> Note that the agentId must be unique, otherwise it will conflict on the tunnel server and not work properly.

If the arthas agent is configured with `appName`, the generated agentId will be prefixed with `appName`.

For example, if you add the startup parameter `as.sh --tunnel-server 'ws://127.0.0.1:7777/ws' --app-name demoapp`, the generated agentId might be `demoapp_URJZ5L48RPBR2ALI5K4V`.

Tunnel server will use `_` as a delimiter to extract `appName`, which is convenient to manage by application.

> Alternatively, you can configure `appName` in `arthas.properties` in the unzipped arthas directory, or in `application.properties` of the spring boot application.


### Cluster Management

If you want to deploy multiple tunnel servers, you can use nginx for forwarding and redis to store agent information.


### How arthas tunnel server works

```
browser <-> arthas tunnel server <-> arthas tunnel client <-> arthas agent
```

[tunnel-server/README.md](https://github.com/alibaba/arthas/blob/master/tunnel-server/README.md#)
