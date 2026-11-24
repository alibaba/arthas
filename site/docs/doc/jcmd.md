# jcmd

通过目标 JVM 的 `DiagnosticCommand` MBean 执行诊断命令。

需要目标 JVM 提供 `com.sun.management:type=DiagnosticCommand`，例如 HotSpot JDK 8 及以上版本。只能执行此 MBean 暴露的命令，可用列表及其参数由目标 JVM 决定，无需指定进程 ID。

## 使用方法

列出可用的诊断命令：

```bash
jcmd
```

查看 JVM 提供的命令帮助，包括参数和执行影响：

```bash
jcmd help VM.flags
```

使用列表中的名称执行命令：

```bash
jcmd VM.version
jcmd Thread.print
```

在以 `-` 开头的诊断选项前加上 `--`，使 Arthas 将这些选项传给 JVM：

```bash
jcmd VM.flags -- -all
jcmd Thread.print -- -l
```

参数中包含空格时，需要为 JVM 的诊断命令解析器保留引号，可以在引号外再包一层引号。例如，在提供 `Thread.dump_to_file` 的 JVM 上：

```bash
jcmd Thread.dump_to_file '"/tmp/thread dump.txt"'
```

`jcmd -h` 显示 Arthas 命令用法；`jcmd help` 显示目标 JVM 支持的诊断命令。诊断命令在当前 JVM 中执行，具体影响见 JVM 提供的命令帮助。
