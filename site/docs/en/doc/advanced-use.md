# Other features

## Arthas Async Jobs

If you need to investigate an issue, but you are unsure about the exact time it occurs, you can run the monitoring command in the background and save the output to a log file.

- [Arthas Async Jobs](async.md)

## Log the output

All execution records are fully saved in the log file for subsequent analysis.

- [log the output](save-log.md)

## Docker

Arthas configuration reference for using in Docker containers.

- [Docker](docker.md)

## Web Console

Arthas supports living inside a browser. The communication between arthas and browser is via websocket.

- [Web Console](web-console.md)

## Arthas Tunnel

Arthas Tunnel Server/Client enables remote management/connection to Java services across multiple servers.

- [Arthas Tunnel](tunnel.md)

## How to use ognl

- [Basic ognl example](https://github.com/alibaba/arthas/issues/11)
- [Ognl special uses](https://github.com/alibaba/arthas/issues/71)

## IDEA Plugin

Build arthas commands more efficiently in the IntelliJ IDEA compiler.

- [IDEA Plugin](idea-plugin.md)

## Arthas Properties

Arthas supports configuration options reference.

- [Arthas Properties](arthas-properties.md)

## Start as a Java Agent

- [Start as a Java Agent](agent.md)

## Arthas Spring Boot Starter

Starting with the application.

- [Arthas Spring Boot Starter](spring-boot-starter.md)

## HTTP API

The Http API provides structured data and supports more complex interactive functions, making it easier to integrate Arthas into custom interfaces.

- [HTTP API](http-api.md)

## Batch Processing

It is convenient for running multiple commands in bulk with custom scripts. It can be used in conjunction with the `--select` parameter to specify the process name.

- [Batch Processing](batch-support.md)

## as.sh and arthas-boot tips

- Select the process to be attached via the `select` option.

Normally, `as.sh`/`arthas-boot.jar` needs to a pid, bacause the pid will change.

For example, with `math-game.jar` already started, use the `jps` command to see.

```bash
$ jps
58883 math-game.jar
58884 Jps
```

The `select` option allows you to specify a process name, which is very convenient.

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

## User data report

After the `3.1.4` version, arthas support user data report.

At startup, use the `stat-url` option, such as: `./as.sh --stat-url 'http://192.168.10.11:8080/api/stat'`

There is a sample data report in the tunnel server that users can implement on their own.

[StatController.java](https://github.com/alibaba/arthas/blob/master/tunnel-server/src/main/java/com/alibaba/arthas/tunnel/server/app/web/StatController.java)
