# jcmd

Execute diagnostic commands in the attached JVM through its `DiagnosticCommand` MBean.

This requires a JVM that provides `com.sun.management:type=DiagnosticCommand`, such as HotSpot JDK 8 or later. Only commands exposed by this MBean are available; the list and options depend on the target JVM. A process ID is not needed.

## Usage

List available diagnostic commands:

```bash
jcmd
```

Show the JVM's help for a command, including its arguments and impact:

```bash
jcmd help VM.flags
```

Run a command using the name shown in the list:

```bash
jcmd VM.version
jcmd Thread.print
```

Use `--` before diagnostic options that start with `-`, so Arthas passes them to the JVM:

```bash
jcmd VM.flags -- -all
jcmd Thread.print -- -l
```

For an argument containing spaces, retain quotes for the JVM's diagnostic command parser by wrapping them in another pair of quotes. For example, on a JVM that provides `Thread.dump_to_file`:

```bash
jcmd Thread.dump_to_file '"/tmp/thread dump.txt"'
```

`jcmd -h` shows the Arthas command usage; `jcmd help` shows the diagnostic commands supported by the JVM. Diagnostic commands run in the current JVM and have the effects described by their JVM help.
