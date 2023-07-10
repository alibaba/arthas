

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


### List all classes loaded in ClassLoader

List all classes loaded by `org.apache.jasper.servlet.JasperLoader`:

`classloader -a --classLoaderClass org.apache.jasper.servlet.JasperLoader`{{execute T2}}

```bash
$ classloader -a --classLoaderClass apache.jasper.servlet.JasperLoader
 hash:1698045338, org.apache.jasper.servlet.JasperLoader@65361d9a
 org.apache.jsp.jsp.hello_jsp
```

* PS: Same as `ognl`, you can also use `-c <hashcode>` instead of `--classLoaderClass` to specify

### Decompile dynamically generated jsp classes

`jad org.apache.jsp.jsp.hello_jsp`{{execute T2}}

```bash
$ jad org.apache.jsp.jsp.hello_jsp

ClassLoader:
+-org.apache.jasper.servlet.JasperLoader@65361d9a
  +-TomcatEmbeddedWebappClassLoader
      context: ROOT
...
```

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

Note: Please replace `<classLoaderHash>` with your classLoaderHash above, then execute the related commands manually in the following steps:

### List the urls of the ClassLoader

For example, the hashcode of spring `LaunchedURLClassLoader` viewed above is `1be6f5c3`, and all its urls can be listed by the `-c` parameter:

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader`{{execute T2}}

```
$ classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader
jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/classes!/
jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/spring-boot-starter-aop-1.5
.13.RELEASE.jar!/
...
```

### Load the resource file in the specified ClassLoader

Load the specified resource file: `classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml`{{execute T2}}

```
$ classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml
 jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/classes!/logback-spring.xml
```

### Try to load the specified class

For example, try loading `java.lang.String` with spring LaunchedURLClassLoader :

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --load java.lang.String`{{execute T2}}

```
$ classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --load java.lang.String
load class success.
 class-info        java.lang.String
 code-source
 name              java.lang.String
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       String
 modifier          final,public
 annotation
 interfaces        java.io.Serializable,java.lang.Comparable,java.lang.CharSequence
 super-class       +-java.lang.Object
 class-loader
 classLoaderHash   null
```

