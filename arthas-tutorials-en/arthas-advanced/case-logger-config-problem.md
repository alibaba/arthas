


In this case, show how to troubleshoot logger conflicts.

### View the logger system used by the app

Take `UserController` as an example, it uses slf4j api, but the actual logger system used is logback.

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}


```bash
$ ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'
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

### Find the configuration file actually loaded by the logback


`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '#map1=@org.slf4j.LoggerFactory@getLogger("root").loggerContext.objectMap, #map1.get("CONFIGURATION_WATCH_LIST")'`{{execute T2}}


### Use the classloader command to find possible logger configuration files

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml`{{execute T2}}

```
$ classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml
 jar:file:/Users/hengyunabc/code/java/spring-boot-inside/demo-arthas-spring-boot/target/demo-arthas-spring-boot-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes!/logback-spring.xml

Affect(row-cnt:1) cost in 13 ms.
```
You can know the specific source of the loaded configuration.

You can try to load files that are prone to conflict:

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback.xml`{{execute T2}}

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r log4j.properties`{{execute T2}}


