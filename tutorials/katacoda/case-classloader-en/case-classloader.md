

The following describes the usage of the `classloader` command.

First visit the jsp page: https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/hello

### List all ClassLoaders

`classloader -l`{{execute T2}}

```bash
$ classloader -l
 name                                                             loadedCount  hash      parent
 BootstrapClassLoader                                             2724         null      null
 com.taobao.arthas.agent.ArthasClassloader@411ce1ab               2009         411ce1ab  sun.misc.Launcher$ExtClassLoader@7494e528
 com.taobao.arthas.agent.ArthasClassloader@22ae1234               1253         22ae1234  sun.misc.Launcher$ExtClassLoader@7494e528
 org.apache.jasper.servlet.JasperLoader@65361d9a                  1            65361d9a  TomcatEmbeddedWebappClassLoader
                                                                                           context: ROOT
                                                                                           delegate: true
                                                                                         ----------> Parent Classloader:
                                                                                         org.springframework.boot.loader.LaunchedURLClassLoader@1be6f5c3

 TomcatEmbeddedWebappClassLoader                                  0            8546cd5   org.springframework.boot.loader.LaunchedURLClassLoader@1be6f5c3
   context: ROOT
   delegate: true
 ----------> Parent Classloader:
 org.springframework.boot.loader.LaunchedURLClassLoader@1be6f5c3

 org.springframework.boot.loader.LaunchedURLClassLoader@1be6f5c3  5416         1be6f5c3  sun.misc.Launcher$AppClassLoader@3d4eac69
 sun.misc.Launcher$AppClassLoader@3d4eac69                        45           3d4eac69  sun.misc.Launcher$ExtClassLoader@7494e528
 sun.misc.Launcher$ExtClassLoader@7494e528                        4            7494e528  null
```

* The number of classes loaded by TomcatEmbeddedWebappClassLoader is 0, so in spring boot embedded tomcat, it is just an empty ClassLoader, all the classes are loaded by `LaunchedURLClassLoader`


Please write down your classLoaderHash here, in the case here, it's `65361d9a`. It will be used in the future steps.

Note: Please replace `<classLoaderHash>` with your classLoaderHash above, then execute the commands manually in the following steps:

### List all classes loaded in ClassLoader

List all classes loaded by `org.apache.jasper.servlet.JasperLoader`:

`classloader -a -c <classLoaderHash>`

```bash
$ classloader -a -c 65361d9a
 hash:1698045338, org.apache.jasper.servlet.JasperLoader@65361d9a
 org.apache.jsp.jsp.hello_jsp
```

### Check the structure of classloader

`sc -d org.apache.jsp.jsp.hello_jsp`{{execute T2}}

### View the ClassLoader tree


`classloader -t`{{execute T2}}

```
$ classloader -t
+-BootstrapClassLoader
+-sun.misc.Launcher$ExtClassLoader@28cbbddd
  +-com.taobao.arthas.agent.ArthasClassloader@8c25e55
  +-sun.misc.Launcher$AppClassLoader@55f96302
    +-org.springframework.boot.loader.LaunchedURLClassLoader@1be6f5c3
      +-TomcatEmbeddedWebappClassLoader
          context: ROOT
          delegate: true
        ----------> Parent Classloader:
        org.springframework.boot.loader.LaunchedURLClassLoader@1be6f5c3

        +-org.apache.jasper.servlet.JasperLoader@21ae0fe2
```

### Show the URLs of the URLClassLoader

For example, the hashcode of spring `LaunchedURLClassLoader` viewed above is `1be6f5c3`, and all its urls can be listed by specifying classloader using the `-c` parameter and then executing the following command:

`classloader -c <classLoaderHash>`

```
$ classloader -c 1be6f5c3
jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/classes!/
jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/spring-boot-starter-aop-1.5
.13.RELEASE.jar!/
...
```

### Load the resource file in the specified ClassLoader

Load the specified resource file: `classloader -c <classLoaderHash> -r logback-spring.xml`

```
$ classloader -c 1be6f5c3 -r logback-spring.xml
 jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/classes!/logback-spring.xml
```

Use the classloader to load .class resource

`classloader -c <classLoaderHash> -r java/lang/String.class`

```bash
$ classloader -c 1b6d3586 -r java/lang/String.class
 jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar!/java/lang/String.class
```

### Try to load the specified class

For example, try loading `ch.qos.logback.classic.spi.StackTraceElementProxy` with spring LaunchedURLClassLoader :

First check with `sc ch.qos.logback.classic.spi.StackTraceElementProxy`{{execute T2}}, you can see that it's unloaded:

`classloader -c <classLoaderHash> --load java.lang.String`
```bash
Affect(row-cnt:0) cost in 18 ms.
```

So use spring LaunchedURLClassLoader to try to load:

`classloader -c <classLoaderHash> --load ch.qos.logback.classic.spi.StackTraceElementProxy`

```bash
$ classloader -c 1be6f5c3 --load ch.qos.logback.classic.spi.StackTraceElementProxy
load class success.
 class-info        ch.qos.logback.classic.spi.StackTraceElementProxy
 code-source       file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/logback-classic-1.
                   1.11.jar!/
 name              ch.qos.logback.classic.spi.StackTraceElementProxy
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       StackTraceElementProxy
 modifier          public
 annotation
 interfaces        java.io.Serializable
 super-class       +-java.lang.Object
 class-loader      +-org.springframework.boot.loader.LaunchedURLClassLoader@5674cd4d
                     +-sun.misc.Launcher$AppClassLoader@70dea4e
                       +-sun.misc.Launcher$ExtClassLoader@56a96482
 classLoaderHash   5674cd4d
```

Finally check with `sc ch.qos.logback.classic.spi.StackTraceElementProxy`{{execute T2}}, and you can see that it has been loaded:

```bash
ch.qos.logback.classic.spi.StackTraceElementProxy
Affect(row-cnt:1) cost in 19 ms.
```
