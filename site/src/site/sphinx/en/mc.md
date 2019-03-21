mc
===

> Memory compiler, compiles `.java` files into `.class` files in memory.

```bash
mc /tmp/Test.java
```

The classloader can be specified with the `-c` option:

```bash
mc -c 327a647b /tmp/Test.java
```

The output directory can be specified with the `-d` option:

```bash
mc -d /tmp/output /tmp/ClassA.java /tmp/ClassB.java
```

After compiling the `.class` file, you can use the [redefine](redefine.md) command to re-define the loaded classes in JVM.

> Note that the mc command may fail. If the compilation fails, the `.class` file can be compiled locally and uploaded to the server. Refer to the [redefine](redefine.md) command description for details.