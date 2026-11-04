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
arthas.sessionTimeout=10800

#arthas.appName=demoapp
#arthas.tunnelServer=ws://127.0.0.1:7777/ws
#arthas.agentId=mmmmmmyiddddd
#arthas.commandLocations=/opt/arthas/ext-command.jar,/opt/arthas/ext-commands
# 默认还会尝试加载 ${arthas.home}/commands 目录下的 *.jar
```

- 如果配置 `arthas.telnetPort`为 -1 ，则不 listen telnet 端口。`arthas.httpPort`类似。
- 如果配置 `arthas.telnetPort`为 0 ，则随机 telnet 端口，在`~/logs/arthas/arthas.log`里可以找到具体端口日志。`arthas.httpPort`类似。

:::tip
如果是防止一个机器上启动多个 arthas 端口冲突。可以配置为随机端口，或者配置为 -1，并且通过 tunnel server 来使用 arthas。
:::

### 最新版本检查

`arthas.checkLatestVersion` 默认为 `true`。Arthas 启动完成后，会在独立的后台守护线程中查询一次最新版本，并将结果写入 `arthas.log`。启动和客户端连接不会等待查询完成，生成欢迎信息也不会重复查询。查询失败只记录 debug 日志，不会重试。

如果不希望目标 JVM 为版本检查发起网络请求，可以在 `arthas.properties` 中配置：

```properties
arthas.checkLatestVersion=false
```

也可以在**目标应用**启动时添加 JVM 参数 `-Darthas.checkLatestVersion=false`。通过 API 初始化时，可以传入：

```java
ArthasBootstrap.getInstance(instrumentation, "checkLatestVersion=false");
```

使用 `Map<String, String>` 重载时，对应的 key 是 `arthas.checkLatestVersion`，value 是 `false`。配置需要在 Arthas 初始化前生效。

后台查询仍然在目标 JVM 内执行，不能隔离 DNS 等原生代码的进程级崩溃。如果版本查询会触发此类问题，应关闭检查；关闭后不会创建版本检查线程，也不会访问 `/api/latest_version`。此配置只控制目标 JVM 内的版本检查，不影响 `arthas-boot` 为选择或下载版本发起的请求。

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

### 加载外部命令

可以通过下面配置在 Arthas 启动时加载外部 command jar：

```
arthas.commandLocations=/opt/arthas/ext-command.jar,/opt/arthas/ext-commands
```

- 支持单个 jar 路径，或目录路径，多个值用英文逗号分隔。
- 目录只会扫描当前目录下的 `*.jar`，不会递归子目录。
- 如果 `${arthas.home}/commands` 目录存在，Arthas 启动时也会自动尝试从该目录加载 `*.jar`。显式配置的 `arthas.commandLocations` 会先加载，再加载默认目录。
- 外部 jar 需要通过 `META-INF/services/com.taobao.arthas.core.shell.command.CommandResolver` 暴露 `CommandResolver` 实现。
- Arthas 会优先保留内置命令；如果外部命令和内置命令重名，会跳过外部命令并记录日志。

也可以在命令行配置： `--command-locations '/opt/arthas/ext-command.jar,/opt/arthas/ext-commands'` 。

完整的外部命令开发和加载示例请参考：[加载外部命令](external-command.md)。

## 配置的优先级

配置的优先级是：命令行参数 > System Env > System Properties > arthas.properties 。

比如：

- `./as.sh --telnet-port 9999` 传入的配置会覆盖掉`arthas.properties`里的默认值`arthas.telnetPort=3658`。
- 如果应用自身设置了 system properties `arthas.telnetPort=8888`，则会覆盖掉`arthas.properties`里的默认值`arthas.telnetPort=3658`。

如果想要 `arthas.properties`的优先级最高，则可以配置 `arthas.config.overrideAll=true` 。
