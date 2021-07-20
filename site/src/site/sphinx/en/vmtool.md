vmtool
===

> @since 3.5.1
[`vmtool` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-vmtool)

`vmtool` uses the `JVMTI` to support `getInstances` in jvm and `forceGc`.

* [JVM Tool Interface](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html)

### getInstances

```bash
$ vmtool --action getInstances --className java.lang.String --limit 10
@String[][
    @String[com/taobao/arthas/core/shell/session/Session],
    @String[com.taobao.arthas.core.shell.session.Session],
    @String[com/taobao/arthas/core/shell/session/Session],
    @String[com/taobao/arthas/core/shell/session/Session],
    @String[com/taobao/arthas/core/shell/session/Session.class],
    @String[com/taobao/arthas/core/shell/session/Session.class],
    @String[com/taobao/arthas/core/shell/session/Session.class],
    @String[com/],
    @String[java/util/concurrent/ConcurrentHashMap$ValueIterator],
    @String[java/util/concurrent/locks/LockSupport],
]
```

> Through the `--limit` parameter, you can limit the number of return values to avoid pressure on the JVM when obtaining large data. The default value of limit is 10.

### Specify classloader name

```bash
vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext
```


### Specify classloader hash

The classloader that loads the class can be found through the `sc` command.

```bash
$ sc -d org.springframework.context.ApplicationContext
 class-info org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext
 code-source file:/private/tmp/demo-arthas-spring-boot.jar!/BOOT-INF/lib/spring-boot-1.5.13.RELEASE.jar!/
 name org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext
...
 class-loader +-org.springframework.boot.loader.LaunchedURLClassLoader@19469ea2
                     +-sun.misc.Launcher$AppClassLoader@75b84c92
                       +-sun.misc.Launcher$ExtClassLoader@4f023edb
 classLoaderHash 19469ea2
```

Then use the `-c`/`--classloader` parameter to specify:

```bash
vmtool --action getInstances -c 19469ea2 --className org.springframework.context.ApplicationContext
```

### Specify the number of expanded layers of returned results

> The return result of the `getInstances` action is bound to the `instances` variable, which is an array.

> The expansion level of the result can be specified by the `-x`/`--expand` parameter, the default value is 1.

```bash
vmtool --action getInstances -c 19469ea2 --className org.springframework.context.ApplicationContext -x 2
```

### Execute expression

> The return result of the `getInstances` action is bound to the `instances` variable, which is an array. The specified expression can be executed through the `--express` parameter.

```bash
vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext --express'instances[0].getBeanDefinitionNames()'
```

### Force GC

```bash
vmtool --action forceGc
```