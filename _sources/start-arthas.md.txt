启动 Arthas
-

## 交互模式启动

> ./as.sh

```sh
➜  bin git:(develop) ✗ ./as.sh
Found existing java process, please choose one and hit RETURN.
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

> ./as.sh \<PID\>[@IP:PORT]



### 参数说明

* PID：目标 Java 进程 ID（请确保执行当前执行命令的用户必须有足够的权限操作对应的 Java 进程）
* IP：目标服务器 IP 地址，当 Arthas Server 启动后，其他人可以通过指定 IP 的形式加载到对应目标机器的 Java 进程中，从而实现远程协助。Arthas 允许多个用户同时访问，并且各自的命令不会相互干扰执行
* PORT：目标服务器 Arthas Server 的端口号，默认的端口号是 3658

### 示例

* 如果不指定 IP 和 PORT，默认是 127.0.0.1 和 3658

	> ./as.sh 12345

	上述命令等价于：
	
	> ./as.sh 12356@127.0.0.1:3658

### 远程诊断

服务器启动 Arthas Server 后，其他人可以使用 Arthas Console Client 远程连接上去进程诊断，请参考：

> ./as.sh PID@RemoteIP:RemotePort
	
其中 PID 为远程服务器上的 Java 进程 ID，RemoteIP 为远程服务器的 IP 地址，RemotePort 为远程服务器上 Arthas Server 的端口，默认为 3658。
	
### sudo 支持

成熟的线上管理环境一般都不会直接开放 JVM 部署用户权限给你，而是通过 sudo-list 来控制和监控用户的越权操作。由于 as.sh 脚本中会对当前用户的环境变量产生感知，所以需要加上 -H 参数

	> sudo -u admin -H ./as.sh 12345

### telnet 的支持

Arthas 支持通过 telnet 来访问服务端，如果当你手头的机器没有安装 Arthas Console Client，你可以简单的通过 telnet 命令来进行访问。

	> telnet 10.232.12.113 3658
	
当然了，telnet 命令没有 `Tab` 自动补全，各种操作体验也不如 Arthas Client Console。

### Windows 环境支持

* 目前 Arthas 提供了 `as.bat` 脚本，用于 Windows 环境下的诊断场景；此脚本暂时只接受一个参数 pid，即只能诊断本机上的 Java 进程；
* 另外，`as.bat` 脚本在体验方面还有一些问题，如果需要更好的体验，可以在本地使用 `as.bat pid` 启动 Arthas Server 后，然后在另外的 Linux/Mac 上使用 as.bat pid@ip:port 来远程诊断；