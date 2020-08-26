mc
===

[`mc-redefine` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-mc-redefine)

> Memory compiler, compiles `.java` files into `.class` files in memory.

```bash
mc /tmp/Test.java
```

The classloader can be specified with the `-c` option:

```bash
mc -c 327a647b /tmp/Test.java
```

You can also specify the ClassLoader with the `--classLoaderClass` option:

```bash
$ mc --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader /tmp/UserController.java -d /tmp
Memory compiler output:
/tmp/com/example/demo/arthas/user/UserController.class
Affect(row-cnt:1) cost in 346 ms
```

The output directory can be specified with the `-d` option:

```bash
mc -d /tmp/output /tmp/ClassA.java /tmp/ClassB.java
```

After compiling the `.class` file, you can use the [redefine](redefine.md) command to re-define the loaded classes in JVM.

> Note that the mc command may fail. If the compilation fails, the `.class` file can be compiled locally and uploaded to the server. Refer to the [redefine](redefine.md) command description for details.