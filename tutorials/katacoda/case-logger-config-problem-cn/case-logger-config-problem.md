

在这个案例里，展示排查logger冲突的方法。

### 查找UserController的ClassLoader

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash
 classLoaderHash   1be6f5c3
```

请记下你的classLoaderHash，后面需要使用它。在这里，它是 `1be6f5c3`。

注意：请使用你的classLoaderHash值覆盖 `<classLoaderHash>` ，然后手动执行下面所有所述命令：

### 确认应用使用的logger系统

以`UserController`为例，它使用的是slf4j api，但实际使用到的logger系统是logback。

`ognl -c <classLoaderHash> '@com.example.demo.arthas.user.UserController@logger'`


```bash
$ ognl -c 1be6f5c3 '@com.example.demo.arthas.user.UserController@logger'
@Logger[
    serialVersionUID=@Long[5454405123156820674],
    FQCN=@String[ch.qos.logback.classic.Logger],
    name=@String[com.example.demo.arthas.user.UserController],
    level=null,
    effectiveLevelInt=@Integer[20000],
    parent=@Logger[Logger[com.example.demo.arthas.user]],
    childrenList=null,
    aai=null,
    additive=@Boolean[true],
    loggerContext=@LoggerContext[ch.qos.logback.classic.LoggerContext[default]],
]
```

### 获取logback实际加载的配置文件


`ognl -c <classLoaderHash> '#map1=@org.slf4j.LoggerFactory@getLogger("root").loggerContext.objectMap, #map1.get("CONFIGURATION_WATCH_LIST")'`


### 使用classloader命令查找可能存在的logger配置文件

`classloader -c <classLoaderHash> -r logback-spring.xml`

```
$ classloader -c 1be6f5c3 -r logback-spring.xml
 jar:file:/Users/hengyunabc/code/java/spring-boot-inside/demo-arthas-spring-boot/target/demo-arthas-spring-boot-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes!/logback-spring.xml

Affect(row-cnt:1) cost in 13 ms.
```
可以知道加载的配置的具体来源。

可以尝试加载容易冲突的文件：

`classloader -c <classLoaderHash> -r logback.xml`

`classloader -c <classLoaderHash> -r log4j.properties`


