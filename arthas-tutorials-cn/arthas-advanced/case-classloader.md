下面介绍`classloader`命令的功能。

先访问一个 jsp 网页，触发 jsp 的加载： [访问 hello 页面]({{TRAFFIC_HOST1_80}}/hello)

### 列出所有 ClassLoader

`classloader -l`{{execute T2}}

### 统计 ClassLoader 实际使用 URL 和未使用的 URL

`classloader --url-stat`{{exec}}

> 注意：基于 JVM 目前已加载的所有类统计，不代表 Unused URLs 可以从应用中删掉。因为可能将来需要从 Unused URLs 里加载类，或者需要加载 resources

### 列出 ClassLoader 里加载的所有类

列出上面的`org.apache.jasper.servlet.JasperLoader`加载的类：

`classloader -a --classLoaderClass org.apache.jasper.servlet.JasperLoader | grep hello`{{exec}}

### 查看类的 classloader 层次

`sc -d org.apache.jsp.jsp.hello_jsp`{{execute T2}}

### 查看 ClassLoader 树

`classloader -t`{{execute T2}}

### 查看 URLClassLoader 实际的 urls

比如上面查看到的 spring LaunchedURLClassLoader 为 `org.springframework.boot.loader.LaunchedURLClassLoader`，可以通过 `-c <hashcode>` 参数来指定 classloader，还有一种方法可以通过使用 `--classLoaderClass` 指定类名，从而查看 URLClassLoader 实际的 urls：

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader`{{exec}}

### 查找 ClassLoader 里的资源文件

查找指定的资源文件： `classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml`{{exec}}

也可以尝试查找类的 class 文件：

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r java/lang/String.class`{{exec}}
