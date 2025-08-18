# qlexpress

[`qlexpress`在线教程](https://github.com/alibaba/QLExpress)

::: tip
执行 qlexpress 表达式
:::

## 参数说明

|                   参数名称 | 参数说明                                                         |
|-----------------------:| :--------------------------------------------------------------- |
|              _express_ | 执行的表达式                                                     |
|                 `[c:]` | 执行表达式的 ClassLoader 的 hashcode，默认值是 SystemClassLoader |
|  `[classLoaderClass:]` | 指定执行表达式的 ClassLoader 的 class name                       |
|                  `[x]` | 结果对象的展开层次，默认值 1                                     |

## 使用参考

- QLExpress 表达式官方指南：[https://github.com/alibaba/QLExpress](https://github.com/alibaba/QLExpress)

调用静态函数：

```bash
$ qlexpress 'java.lang.System.out.println("hello~")'
null
```

获取静态类的静态字段：

```bash
$ qlexpress 'com.taobao.arthas.core.GlobalOptions.isDump'
```

导入应用中的类并且使用：

```bash
$ qlexpress 'import com.alibaba.qlexpress4.QLImportTester; QLImportTester.add(1,2);'
```


通过 hashcode 指定 ClassLoader：

```bash
$ classloader -t
+-BootstrapClassLoader
+-jdk.internal.loader.ClassLoaders$PlatformClassLoader@301ec38b
  +-com.taobao.arthas.agent.ArthasClassloader@472067c7
  +-jdk.internal.loader.ClassLoaders$AppClassLoader@4b85612c
    +-org.springframework.boot.loader.LaunchedURLClassLoader@7f9a81e8

$ qlexpress -c 7f9a81e8 @org.springframework.boot.SpringApplication@logger
@Slf4jLocationAwareLog[
    FQCN=@String[org.apache.commons.logging.LogAdapter$Slf4jLocationAwareLog],
    name=@String[org.springframework.boot.SpringApplication],
    logger=@Logger[Logger[org.springframework.boot.SpringApplication]],
]
$
```

注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。

对于只有唯一实例的 ClassLoader 可以通过 class name 指定，使用起来更加方便：

```bash
$ qlexpress --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader  @org.springframework.boot.SpringApplication@logger
@Slf4jLocationAwareLog[
    FQCN=@String[org.apache.commons.logging.LogAdapter$Slf4jLocationAwareLog],
    name=@String[org.springframework.boot.SpringApplication],
    logger=@Logger[Logger[org.springframework.boot.SpringApplication]],
]
```

执行多行表达式，赋值给临时变量，返回一个 List：

```bash
$ qlexpress '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'
@ArrayList[
    @String[/opt/java/8.0.181-zulu/jre],
    @String[OpenJDK Runtime Environment],
]
```
