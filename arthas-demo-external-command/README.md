# Arthas Demo External Command

这个模块提供了一个最小可运行的外部 command 示例：

- 命令名：`demo-external`
- `Command` 实现：`demo.command.DemoExternalCommand`
- SPI `CommandResolver`：`demo.command.DemoExternalCommandResolver`
- SPI 文件：`src/main/resources/META-INF/services/com.taobao.arthas.core.shell.command.CommandResolver`

构建：

```bash
./mvnw -pl arthas-demo-external-command -DskipTests package
```

把产物 jar 放到 `${arthas.home}/commands/` 目录下，然后启动 Arthas，即可执行：

```bash
demo-external
demo-external Codex
```
