启动 Arthas
=====

## 交互模式启动

```bash
./as.sh
```

```bash
➜  bin git:(develop) ✗ ./as.sh
Found existing java process, please choose one and input the serial number of the process, eg: 1 . Then hit ENTER.
  [1]: 3088 org.jetbrains.idea.maven.server.RemoteMavenServer
* [2]: 12872 org.apache.catalina.startup.Bootstrap
  [3]: 2455
Attaching to 12872...
  ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
 /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
|  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
|  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
`--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'
$
```

## 非交互模式启动

启动脚本如下：

```bash
./as.sh <PID>[@IP:PORT]
```



### 参数说明

* PID：目标 Java 进程 ID（请确保执行当前执行命令的用户必须有足够的权限操作对应的 Java 进程）
* IP：Arthas Server 侦听的地址，默认值是 `127.00.1`。Arthas允许多个用户同时访问，并且各自的命令不会相互干扰执行
* PORT：目标服务器 Arthas Server 的端口号，默认的端口号是 3658

### 示例

* 如果不指定 IP 和 PORT，默认是 127.0.0.1 和 3658

	> ./as.sh 12345

	上述命令等价于：
	
	> ./as.sh 12356@127.0.0.1:3658

### 远程诊断

服务器启动 Arthas Server 后，其他人可以使用 telnet 远程连接上去进程诊断，例如：

```bash
telnet 192.168.1.119 3658
```
	
### sudo 支持

成熟的线上管理环境一般都不会直接开放 JVM 部署用户权限给你，而是通过 sudo-list 来控制和监控用户的越权操作。由于 as.sh 脚本中会对当前用户的环境变量产生感知，所以需要加上 -H 参数

```bash
sudo -u admin -H ./as.sh 12345
```


### Windows 环境支持

目前`as.bat`脚本只支持一个参数：pid

```bash
as.bat <pid>
```
