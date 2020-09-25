Web Console
===

[`Web Console` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=case-web-console)

### Connect arthas through the browser

Arthas supports the Web Console. After attach success, the user can access: [http://127.0.0.1:3658/](http://127.0.0.1:3658/).

The user can fill in the IP and connect the remote arthas on other machines.

![](_static/web-console-local.png)

> By default, arthas only listens to `127.0.0.1`, so if you want to connect from a remote, you can use the `--target-ip` parameter to specify the IP. See the help description for `-h` for more information.

If you have suggestions for the Web Console, please leave a message here: [https://github.com/alibaba/arthas/issues/15](https://github.com/alibaba/arthas/issues/15)

### Connect remote arthas through arthas tunnel server

#### Download and deploy arthas tunnel server

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

#### Connecting to the tunnel server when starting arthas


When starting arthas, you can use the `--tunnel-server` parameter, for example:

```bash
as.sh --tunnel-server 'ws://127.0.0.1:7777/ws'
```

You can also use the following test address (not guaranteed to be available all the time):

```bash
as.sh --tunnel-server 'ws://47.75.156.201:7777/ws'
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
 TUNNEL_SERVER  ws://47.75.156.201:7777/ws
```


For the above example, go to [http://47.75.156.201:8080/](http://47.75.156.201:8080/) in the browser and input the `agentId` to connect to arthas on remote machine.


![](_static/arthas-tunnel-server.png)


#### How arthas tunnel server works

```
browser <-> arthas tunnel server <-> arthas tunnel client <-> arthas agent
```

[tunnel-server/README.md](https://github.com/alibaba/arthas/blob/master/tunnel-server/README.md#)