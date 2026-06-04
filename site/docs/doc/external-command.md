# 加载外部命令

Arthas 支持在启动时加载外部 command jar，用于把团队内部常用的诊断动作封装成新的 Arthas 命令。外部命令加载后可以像内置命令一样通过 `help` 查看和执行。

::: tip
外部命令只在 Arthas 服务端启动时加载。Arthas 已经 attach 到目标 JVM 后，再把 jar 放到目录里不会自动生效，需要重新启动 Arthas 服务端。
:::

## 加载方式

### 通过命令行指定

`arthas-boot.jar` 支持 `--command-locations` 参数，参数值可以是 jar 文件路径，也可以是目录路径，多个路径用英文逗号分隔：

```bash
java -jar arthas-boot.jar --command-locations '/opt/arthas/ext-command.jar,/opt/arthas/ext-commands' <pid>
```

如果使用完整包里的 `as.sh`，也可以传入相同参数：

```bash
./as.sh --command-locations '/opt/arthas/ext-command.jar,/opt/arthas/ext-commands' <pid>
```

### 通过 arthas.properties 指定

在 `arthas.properties` 中配置：

```properties
arthas.commandLocations=/opt/arthas/ext-command.jar,/opt/arthas/ext-commands
```

如果通过 `arthas-spring-boot-starter` 启动，Spring Boot 配置文件中推荐使用 `arthas.command-locations`：

```properties
arthas.command-locations=/opt/arthas/ext-command.jar,/opt/arthas/ext-commands
```

### 默认 commands 目录

如果 `${arthas.home}/commands` 目录存在，Arthas 启动时也会自动加载该目录下的 `*.jar`：

```bash
mkdir -p ${arthas.home}/commands
cp arthas-demo-external-command.jar ${arthas.home}/commands/
```

显式配置的 `arthas.commandLocations` 会先加载，随后再加载默认的 `${arthas.home}/commands` 目录。

## 目录扫描规则

- 单个路径可以指向 jar 文件，也可以指向目录。
- 目录只扫描当前目录下的 `*.jar`，不会递归扫描子目录。
- 同一个 jar 路径会按规范化后的绝对路径去重。
- 如果外部命令依赖第三方 jar，可以把依赖打包进命令 jar，或把依赖 jar 一起放到被扫描的目录中。

## 编写外部命令

外部命令 jar 通过 Java SPI 暴露 `CommandResolver`。一个最小命令通常包含三个部分：

1. 实现一个命令类，继承 `AnnotatedCommand`。
2. 实现 `CommandResolver`，返回命令列表。
3. 在 `META-INF/services/com.taobao.arthas.core.shell.command.CommandResolver` 中写入 `CommandResolver` 实现类名。

示例命令：

```java
package demo.command;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

@Name("demo-external")
@Summary("Demo external command loaded from arthas.home/commands")
@Description("Examples:\n"
        + "  demo-external\n"
        + "  demo-external Codex\n")
public class DemoExternalCommand extends AnnotatedCommand {

    private String message;

    @Argument(index = 0, argName = "message", required = false)
    @Description("message printed by the demo external command")
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void process(CommandProcess process) {
        String value = message;
        if (value == null || value.trim().isEmpty()) {
            value = "hello";
        }
        process.write("demo external command loaded: " + value + "\n");
        process.end();
    }
}
```

示例 `CommandResolver`：

```java
package demo.command;

import java.util.Collections;
import java.util.List;

import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;

public class DemoExternalCommandResolver implements CommandResolver {

    @Override
    public List<Command> commands() {
        return Collections.singletonList(Command.create(DemoExternalCommand.class));
    }
}
```

SPI 文件路径：

```text
src/main/resources/META-INF/services/com.taobao.arthas.core.shell.command.CommandResolver
```

SPI 文件内容：

```text
demo.command.DemoExternalCommandResolver
```

## Maven 依赖

外部命令需要依赖 Arthas 的命令接口和 CLI 注解。建议将这些依赖声明为 `provided`，避免把 Arthas 自身类重复打包进外部命令 jar：

```xml
<dependencies>
    <dependency>
        <groupId>com.taobao.arthas</groupId>
        <artifactId>arthas-core</artifactId>
        <version>${arthas.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.alibaba.middleware</groupId>
        <artifactId>cli</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Arthas 仓库中提供了完整示例：[arthas-demo-external-command](https://github.com/alibaba/arthas/tree/master/arthas-demo-external-command)。

## 构建和验证

以示例模块为例，构建命令 jar：

```bash
./mvnw -pl arthas-demo-external-command -DskipTests package
```

将生成的 jar 放到 `${arthas.home}/commands/` 后启动 Arthas：

```bash
mkdir -p ${arthas.home}/commands
cp arthas-demo-external-command/target/arthas-demo-external-command-*.jar ${arthas.home}/commands/
java -jar arthas-boot.jar <pid>
```

进入 Arthas 后执行：

```bash
help demo-external
demo-external Codex
```

预期可以看到类似输出：

```text
demo external command loaded: Codex
```

## 加载行为和冲突处理

Arthas 启动时会把外部 command jar 加入 Arthas ClassLoader，然后通过 `ServiceLoader<CommandResolver>` 发现命令。命令注册时遵循下面规则：

- 外部命令不能覆盖 Arthas 内置命令；如果和内置命令重名，外部命令会被跳过并记录日志。
- 多个外部命令重名时，只保留第一个命令，后续重名命令会被跳过并记录日志。
- `CommandResolver` 加载失败不会中断其他 resolver 的加载，错误会写入 Arthas 日志。
- 如果没有发现有效的外部命令，Arthas 会继续按正常流程启动。
