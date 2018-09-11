Arthas 3.0使用说明
===

## 命令行诊断

1. 安装Arthas 3.0启动脚本

    ```
curl -sLk http://TODO/arthas/install.sh | sh
```

2. 可以直接运行`./as.sh`，启动脚本会自动更新到最新版本

```
➜  tmp ./as.sh
Found existing java process, please choose one and hit RETURN.
* [1]: 53090 org.apache.catalina.startup.Bootstrap
  [2]: 50020
  [3]: 69893 org.jetbrains.idea.maven.server.RemoteMavenServer
  [4]: 93320 org.jetbrains.jps.cmdline.Launcher
updating version 3.0.20170224142641 ...
######################################################################## 100.0%
Archive:  /var/tmp/temp_3.0.20170224142641_35932/arthas-3.0.20170224142641-bin.zip
   creating: /var/tmp/temp_3.0.20170224142641_35932/arthas/
  inflating: /var/tmp/temp_3.0.20170224142641_35932/arthas/arthas-agent.jar
  inflating: /var/tmp/temp_3.0.20170224142641_35932/arthas/arthas-client.jar
  inflating: /var/tmp/temp_3.0.20170224142641_35932/arthas/arthas-core.jar
  inflating: /var/tmp/temp_3.0.20170224142641_35932/arthas/arthas-spy.jar
  inflating: /var/tmp/temp_3.0.20170224142641_35932/arthas/as.bat
  inflating: /var/tmp/temp_3.0.20170224142641_35932/arthas/as.sh
  inflating: /var/tmp/temp_3.0.20170224142641_35932/arthas/install-local.sh
update completed.
Calculating attach excecution time...
Attaching to 53090...
real    0m0.336s
user    0m0.498s
sys     0m0.075s
Attach success.
Connecting to arthas server... current timestamp is 1488178414
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'
wiki: https://github.com/alibaba/arthas/wiki/
version: 3.0.20170227131708
pid: 53090
timestamp: 1488178414548
```

### telnet远程诊断

只要在上一步中启动了Arthas Server， 那么你可以直接在本地（针对日常环境的机器），或者是跳板机（针对预发和线上机器）上，通过telnet命令直接远程连上去诊断，使用上的体验完全一致。

```
➜  telnet 11.164.45.24 3658
Trying 11.164.45.24...
Connected to 11.164.45.24.
Escape character is '^]'.
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'


wiki: https://github.com/alibaba/arthas/wiki/home
version: 3.0.20170315140000
pid: 1463
timestamp: 1490938898194

$

```

## 新特性介绍

请参考[Arthas 3.0新特性](https://github.com/alibaba/arthas/wiki/arthas_3_0/new_feature)

## FAQ

首先请确保你的目标机器上，Arthas 2.0的server正常的shutdown了。否则，Arthas 3.0连上去之后不会正常启动。

### 关于web console中文本的复制粘贴

目前不同浏览器对复制粘贴的支持程度各不相同，mac下的Chrome是支持的最好的，目前的支持情况如下，我们正在想办法提供更好的复制粘贴体验

| 操作系统    | 浏览器     | 复制             | 粘贴               |
| ------- | ------- | -------------- | ---------------- |
| macOS   | Chrome  | COMMAND-C，鼠标右键-复制 | COMMAND-V，鼠标右键-粘贴   |
| macOS   | Safari  | 鼠标右键-复制           | 鼠标右键-粘贴            |
| macOS   | Firefox |                |                  |
| Windows | Chrome  | Ctrl + C           |  Ctrl + Shift + V |
| Windows | Firefox  |            |  |

### 关于字体大小

如果觉得字体太小，可以通过浏览器内置的字体调整功能进行，web console支持自适应调整。

| 操作系统    | 浏览器     | 增大字体             | 减小字体               |
| ------- | ------- | -------------- | ---------------- |
| macOS   | Chrome  | COMMAND加= | COMMAND加-   |
| macOS   | Safari  | COMMAND加= | COMMAND加-   |            |
| macOS   | Firefox |                |                  |
| Windows | Chrome  |            |  |
| Windows | Firefox  |            |  |