# Load External Commands

Arthas can load external command jars at startup. This is useful when you want to package team-specific diagnostic actions as Arthas commands. After an external command is loaded, you can use it like a built-in command and inspect it with `help`.

::: tip
External commands are loaded only when the Arthas server starts. If Arthas has already attached to the target JVM, copying a jar into the command directory will not take effect until the Arthas server is restarted.
:::

## Loading methods

### Command line

`arthas-boot.jar` supports the `--command-locations` option. Each entry can be a jar file path or a directory path, separated by commas:

```bash
java -jar arthas-boot.jar --command-locations '/opt/arthas/ext-command.jar,/opt/arthas/ext-commands' <pid>
```

If you use `as.sh` from the full distribution, you can pass the same option:

```bash
./as.sh --command-locations '/opt/arthas/ext-command.jar,/opt/arthas/ext-commands' <pid>
```

### arthas.properties

Configure `arthas.commandLocations` in `arthas.properties`:

```properties
arthas.commandLocations=/opt/arthas/ext-command.jar,/opt/arthas/ext-commands
```

If you start Arthas through `arthas-spring-boot-starter`, use `arthas.command-locations` in Spring Boot configuration files:

```properties
arthas.command-locations=/opt/arthas/ext-command.jar,/opt/arthas/ext-commands
```

### Default commands directory

If `${arthas.home}/commands` exists, Arthas also loads `*.jar` from that directory at startup:

```bash
mkdir -p ${arthas.home}/commands
cp arthas-demo-external-command.jar ${arthas.home}/commands/
```

Explicit `arthas.commandLocations` are loaded first, followed by the default `${arthas.home}/commands` directory.

## Directory scanning rules

- Each location can point to a jar file or a directory.
- Directory entries scan only `*.jar` files in the current directory. Subdirectories are not scanned recursively.
- The same jar path is deduplicated by its canonical absolute path.
- If an external command depends on third-party jars, package the dependencies into the command jar, or put those dependency jars in a scanned directory.

## Writing an external command

External command jars expose `CommandResolver` implementations through Java SPI. A minimal command usually has three parts:

1. A command class that extends `AnnotatedCommand`.
2. A `CommandResolver` implementation that returns the command list.
3. A `META-INF/services/com.taobao.arthas.core.shell.command.CommandResolver` file that contains the resolver class name.

Example command:

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

Example `CommandResolver`:

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

SPI file path:

```text
src/main/resources/META-INF/services/com.taobao.arthas.core.shell.command.CommandResolver
```

SPI file content:

```text
demo.command.DemoExternalCommandResolver
```

## Maven dependencies

External commands depend on Arthas command interfaces and CLI annotations. These dependencies should usually use `provided` scope to avoid packaging Arthas classes into the external command jar:

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

The Arthas repository provides a complete example: [arthas-demo-external-command](https://github.com/alibaba/arthas/tree/master/arthas-demo-external-command).

## Build and verify

Use the demo module as an example:

```bash
./mvnw -pl arthas-demo-external-command -DskipTests package
```

Copy the generated jar to `${arthas.home}/commands/` and start Arthas:

```bash
mkdir -p ${arthas.home}/commands
cp arthas-demo-external-command/target/arthas-demo-external-command-*.jar ${arthas.home}/commands/
java -jar arthas-boot.jar <pid>
```

Run the command in Arthas:

```bash
help demo-external
demo-external Codex
```

Expected output:

```text
demo external command loaded: Codex
```

## Loading behavior and conflicts

At startup, Arthas appends external command jars to the Arthas ClassLoader and discovers commands with `ServiceLoader<CommandResolver>`. Command registration follows these rules:

- External commands cannot override built-in commands. If an external command has the same name as a built-in command, it is skipped and a log is written.
- If multiple external commands have the same name, only the first command is kept. Later duplicates are skipped and logged.
- A broken `CommandResolver` does not stop other resolvers from loading. The error is written to the Arthas log.
- If no valid external command is found, Arthas continues to start normally.
