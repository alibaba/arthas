# Arthas Properties

`arthas.properties`文件在 arthas 的目录下。

- 如果是自动下载的 arthas，则目录在`~/.arthas/lib/3.x.x/arthas/`下面
- 如果是下载的完整包，在 arthas 解压目录下

## 支持的配置项

::: warning
注意配置必须是`驼峰`的，和 spring boot 的`-`风格不一样。spring boot 应用才同时支持`驼峰` 和 `-`风格的配置。
:::

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

- 如果配置 `arthas.telnetPort`为 -1 ，则不 listen telnet 端口。`arthas.httpPort`类似。
- 如果配置 `arthas.telnetPort`为 0 ，则随机 telnet 端口，在`~/logs/arthas/arthas.log`里可以找到具体端口日志。`arthas.httpPort`类似。

:::tip
如果是防止一个机器上启动多个 arthas 端口冲突。可以配置为随机端口，或者配置为 -1，并且通过 tunnel server 来使用 arthas。
:::

### 禁止指定命令

::: tip
since 3.5.2
:::

比如配置：

```
arthas.disabledCommands=stop,dump
```

也可以在命令行配置： `--disabled-commands stop,dump` 。

::: tip
默认情况下，arthas-spring-boot-starter 会禁掉`stop`命令。
:::

## 配置的优先级

配置的优先级是：命令行参数 > System Env > System Properties > arthas.properties 。

比如：

- `./as.sh --telnet-port 9999` 传入的配置会覆盖掉`arthas.properties`里的默认值`arthas.telnetPort=3658`。
- 如果应用自身设置了 system properties `arthas.telnetPort=8888`，则会覆盖掉`arthas.properties`里的默认值`arthas.telnetPort=3658`。

如果想要 `arthas.properties`的优先级最高，则可以配置 `arthas.config.overrideAll=true` 。
