## Use specified pid

You can use`jps`{{execute T2}} to check target pid。

`java -jar arthas-boot.jar 1`{{execute T2}}

## Allow external network access

By default, the arthas server listens for the IP of `127.0.0.1`. If you want remote access, you can use the `--target-ip` option.

`java -jar arthas-boot.jar --target-ip`{{execute T2}}

## Specify listening port

By default, the arthas server listens for the telnet port `3658`，http port `8563`，you can use `--telnet-port`，`--http-port` to specify.

Only listens at the Telnet port and does not listen at the HTTP port:

`java -jar arthas-boot.jar --telnet-port 9999 --http-port -1`{{execute T2}}

## Specify tunnel server

Use `--tunnel-server` parameter to specify.

`java -jar arthas-boot.jar --tunnel-server 'ws://192.168.10.11:7777/ws'`{{execute T2}}

If the tunnel server has registered with agent id，you can use `--agent-id` parameter to specify.

`java -jar arthas-boot.jar --tunnel-server 'ws://192.168.10.11:7777/ws' --agent-id bvDOe8XbTM2pQWjF4cfw'`{{execute T2}}

## Specify report stat url

Use `--stat-url` parameter to specify.

`java -jar arthas-boot.jar --stat-url 'http://192.168.10.11:8080/api/stat'`{{execute T2}}

## List all versions

`java -jar arthas-boot.jar --versions`{{execute T2}}

Use the specified version:

`java -jar arthas-boot.jar --use-version 3.1.0`{{execute T2}}

## Print verbose information

Use `-v` or `-verbose`:

`java -jar arthas-boot.jar -v`{{execute T2}}

## Specify command to execute and target pid

Use `--command` or `-c` to specify command to execute and target pid, multiple commands separated by `;`.

`java -jar arthas-boot.jar -c 'sysprop; thread' 1`{{execute T2}}

## Specify batch file to execute and target pid

Use `--batch-file` or `-f` to specify batch file to execute and target pid.

`java -jar arthas-boot.jar -f batch.as 1`{{execute T2}}

## select target process by classname or JARfilename

Use `--select` to select target process by classname or JARfilename.

`java -jar arthas-boot.jar --select math-game`{{execute T2}}

## Specify session timeout seconds

Use `--session-timeout`parameter to specify，default value is 1800(30 min).

`java -jar arthas-boot.jar --session-timeout 3600`{{execute T2}}

## Attach target process only, do not connect

`java -jar arthas-boot.jar --attach-only`{{execute T2}}

## Use special maven repository mirror，Enforce use http

`--repo-mirror` to use special maven repository mirror，value is `center/aliyun` or http repo url.

`--use-http` to enforce use http to download, default use https

`java -jar arthas-boot.jar --repo-mirror aliyun --use-http`{{execute T2}}

## Specify arthas client terminal height and width

`java -jar arthas-boot.jar --height 25 --width 80`{{execute T2}}

## Specify arthas home

`java -jar arthas-boot.jar --arthas-home .`{{execute T2}}

## Start as a Java Agent

Usually Arthas dynamic attach the applications on the fly, but from version 3.2.0 onwards, Arthas supports starting directly as a java agent.

For example, download the full arthas zip package, decompress it and start it by specifying arthas-agent.jar with the parameter -javaagent.

`java -javaagent:/tmp/test/arthas-agent.jar -jar math-game.jar`

The default configuration is in the arthas.properties file in the decompression directory.

Reference: https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html
