

下面介绍`classloader`命令的功能。

先访问一个jsp网页，触发jsp的加载： https://[[HOST_SUBDOMAIN]]-80-[[KATACODA_HOST]].environments.katacoda.com/hello

### 列出所有ClassLoader

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

* TomcatEmbeddedWebappClassLoader 加载的class数量是0，所以在spring boot embedded tomcat里，它只是一个空壳，所有的类加载都是`LaunchedURLClassLoader`完成的

请记下你的classLoaderHash，后面需要使用它。在这里，它是 `65361d9a`。

注意：请使用你的classLoaderHash值覆盖 `<classLoaderHash>` ，然后手动执行下面所有所述命令：

### 列出ClassLoader里加载的所有类

列出上面的`org.apache.jasper.servlet.JasperLoader`加载的类：

`classloader -a -c <classLoaderHash>`

```bash
$ classloader -a -c 65361d9a
 hash:1698045338, org.apache.jasper.servlet.JasperLoader@65361d9a
 org.apache.jsp.jsp.hello_jsp
```

### 查看类的classloader层次

`sc -d org.apache.jsp.jsp.hello_jsp`{{execute T2}}

### 查看ClassLoader树


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

### 查看URLClassLoader实际的urls

比如上面查看到的spring LaunchedURLClassLoader的 hashcode是`1be6f5c3`，可以通过`-c`参数来指定classloader，从而查看URLClassLoader实际的urls：

`classloader -c <classLoaderHash>`

```
$ classloader -c 1be6f5c3
jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/classes!/
jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/spring-boot-starter-aop-1.5
.13.RELEASE.jar!/
...
```

### 加载指定ClassLoader里的资源文件

查找指定的资源文件： `classloader -c <classLoaderHash> -r logback-spring.xml`

```
$ classloader -c 1be6f5c3 -r logback-spring.xml
 jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/classes!/logback-spring.xml
```
也可以尝试查找类的class文件：

`classloader -c <classLoaderHash> -r java/lang/String.class`

```bash
$ classloader -c 1b6d3586 -r java/lang/String.class
 jar:file:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar!/java/lang/String.class
```

### 尝试加载指定的类

比如用上面的spring LaunchedURLClassLoader 尝试加载 `ch.qos.logback.classic.spi.StackTraceElementProxy` ：

首先使用`sc ch.qos.logback.classic.spi.StackTraceElementProxy`{{execute T2}}查看，可发现未加载：

```bash
Affect(row-cnt:0) cost in 18 ms.
```

因而使用spring LaunchedURLClassLoader 尝试加载：

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

再次使用`sc ch.qos.logback.classic.spi.StackTraceElementProxy`{{execute T2}}查看，发现已经加载：

```bash
ch.qos.logback.classic.spi.StackTraceElementProxy
Affect(row-cnt:1) cost in 19 ms.
```
