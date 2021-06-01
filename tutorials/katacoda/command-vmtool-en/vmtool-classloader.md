### Specify classloader name

`vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext`{{execute T2}}

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