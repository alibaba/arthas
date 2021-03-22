

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


### 列出ClassLoader里加载的所有类

列出上面的`org.apache.jasper.servlet.JasperLoader`加载的类：

`classloader -a --classLoaderClass org.apache.jasper.servlet.JasperLoader`{{execute T2}}

```bash
$ classloader -a --classLoaderClass apache.jasper.servlet.JasperLoader
 hash:1698045338, org.apache.jasper.servlet.JasperLoader@65361d9a
 org.apache.jsp.jsp.hello_jsp
```

* 注：同ognl, 也可用`-c <hashcode>`而不用`--classLoaderClass`指定

### 反编译jsp的代码

`jad org.apache.jsp.jsp.hello_jsp`{{execute T2}}

```bash
$ jad org.apache.jsp.jsp.hello_jsp

ClassLoader:
+-org.apache.jasper.servlet.JasperLoader@65361d9a
  +-TomcatEmbeddedWebappClassLoader
      context: ROOT
...
```

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
注意：请使用你的classLoaderHash值覆盖 `<classLoaderHash>` ，然后手动执行下面相关命令：

### 列出ClassLoader的urls

比如上面查看到的spring LaunchedURLClassLoader的 hashcode是`1be6f5c3`，可以通过`-c`或者`--classLoaderClass`参数来列出它的所有urls：

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader`{{execute T2}}

```
$ classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader
jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/classes!/
jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/spring-boot-starter-aop-1.5
.13.RELEASE.jar!/
...
```

### 加载指定ClassLoader里的资源文件

查找指定的资源文件： `classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml`{{execute T2}}

```
$ classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml
 jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/classes!/logback-spring.xml
```

### 尝试加载指定的类

比如用上面的spring LaunchedURLClassLoader 尝试加载 `java.lang.String` ：

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

