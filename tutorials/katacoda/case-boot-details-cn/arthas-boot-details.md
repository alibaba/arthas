
在新的`Terminal 2`里，下载`arthas-boot.jar`：

`wget https://arthas.aliyun.com/arthas-boot.jar`{{execute T2}}

`arthas-boot.jar` 支持很多参数，可以执行 `java -jar arthas-boot.jar -h`{{execute T2}} 来查看。

```bash
$ java -jar arthas-boot.jar -h
[INFO] arthas-boot version: 3.3.6
Usage: arthas-boot [-h] [--target-ip <value>] [--telnet-port <value>]
       [--http-port <value>] [--session-timeout <value>] [--arthas-home <value>]
       [--use-version <value>] [--repo-mirror <value>] [--versions] [--use-http]
       [--attach-only] [-c <value>] [-f <value>] [--height <value>] [--width
       <value>] [-v] [--tunnel-server <value>] [--agent-id <value>] [--stat-url
       <value>] [--select <value>] [pid]

Bootstrap Arthas

EXAMPLES:
  java -jar arthas-boot.jar <pid>
  java -jar arthas-boot.jar --target-ip 0.0.0.0
  java -jar arthas-boot.jar --telnet-port 9999 --http-port -1
  java -jar arthas-boot.jar --tunnel-server 'ws://192.168.10.11:7777/ws'
  java -jar arthas-boot.jar --tunnel-server 'ws://192.168.10.11:7777/ws'
--agent-id bvDOe8XbTM2pQWjF4cfw
  java -jar arthas-boot.jar --stat-url 'http://192.168.10.11:8080/api/stat'
  java -jar arthas-boot.jar -c 'sysprop; thread' <pid>
  java -jar arthas-boot.jar -f batch.as <pid>
  java -jar arthas-boot.jar --use-version 3.3.6
  java -jar arthas-boot.jar --versions
  java -jar arthas-boot.jar --select math-game
  java -jar arthas-boot.jar --session-timeout 3600
  java -jar arthas-boot.jar --attach-only
  java -jar arthas-boot.jar --repo-mirror aliyun --use-http
WIKI:
  https://arthas.aliyun.com/doc

Options and Arguments:
 -h,--help                      Print usage
    --target-ip <value>         The target jvm listen ip, default 127.0.0.1
    --telnet-port <value>       The target jvm listen telnet port, default 3658
    --http-port <value>         The target jvm listen http port, default 8563
    --session-timeout <value>   The session timeout seconds, default 1800
                                (30min)
    --arthas-home <value>       The arthas home
    --use-version <value>       Use special version arthas
    --repo-mirror <value>       Use special maven repository mirror, value is
                                center/aliyun or http repo url.
    --versions                  List local and remote arthas versions
    --use-http                  Enforce use http to download, default use https
    --attach-only               Attach target process only, do not connect
 -c,--command <value>           Command to execute, multiple commands separated
                                by ;
 -f,--batch-file <value>        The batch file to execute
    --height <value>            arthas-client terminal height
    --width <value>             arthas-client terminal width
 -v,--verbose                   Verbose, print debug info.
    --tunnel-server <value>     The tunnel server url
    --agent-id <value>          The agent id register to tunnel server
    --stat-url <value>          The report stat url
    --select <value>            select target process by classname or
                                JARfilename
 <pid>                          Target pid
 ```
