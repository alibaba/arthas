Arthas Properties
===

`arthas.properties`文件在arthas的目录下。

* 如果是自动下载的arthas，则目录在`~/.arthas/lib/3.x.x/arthas/`下面
* 如果是下载的完整包，在arthas解压目录下


## 支持的配置项


> 注意配置必须是`驼峰`的，和spring boot的`-`风格不一样。spring boot应用才同时支持`驼峰` 和 `-`风格的配置。

```
#arthas.config.overrideAll=true
arthas.telnetPort=3658
arthas.httpPort=8563
arthas.ip=127.0.0.1

# seconds
arthas.sessionTimeout=1800

#arthas.appName=demoapp
#arthas.tunnelServer=ws://127.0.0.1:7777/ws
#arthas.agentId=mmmmmmyiddddd
```

* 如果配置 `arthas.telnetPort`为 -1 ，则不listen telnet端口。`arthas.httpPort`类似。
* 如果配置 `arthas.telnetPort`为 0 ，则随机telnet端口，在`~/logs/arthas/arthas.log`里可以找到具体端口日志。`arthas.httpPort`类似。


> 如果是防止一个机器上启动多个 arthas端口冲突。可以配置为随机端口，或者配置为 -1，并且通过tunnel server来使用arthas。

## 配置的优先级

配置的优先级是：命令行参数 > System Env > System Properties > arthas.properties 。

比如：

* `./as.sh --telnet-port 9999` 传入的配置会覆盖掉`arthas.properties`里的默认值`arthas.telnetPort=3658`。
* 如果应用自身设置了 system properties `arthas.telnetPort=8888`，则会覆盖掉`arthas.properties`里的默认值`arthas.telnetPort=3658`。

如果想要 `arthas.properties`的优先级最高，则可以配置 `arthas.config.overrideAll=true` 。

