


In this case, show how to troubleshoot logger conflicts.

### Find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash
 classLoaderHash   1be6f5c3
```

Please write down your classLoaderHash here, in the case here, it's `1be6f5c3`. It will be used in the future steps.

Note: Please replace `<classLoaderHash>` with your classLoaderHash above, then execute the commands manually in the following steps:

### View the logger system used by the app

Take `UserController` as an example, it uses slf4j api, but the actual logger system used is logback.

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

### Find the configuration file actually loaded by the logback


`ognl -c <classLoaderHash> '#map1=@org.slf4j.LoggerFactory@getLogger("root").loggerContext.objectMap, #map1.get("CONFIGURATION_WATCH_LIST")'`


### Use the classloader command to find possible logger configuration files

`classloader -c <classLoaderHash> -r logback-spring.xml`

```
$ classloader -c 1be6f5c3 -r logback-spring.xml
 jar:file:/Users/hengyunabc/code/java/spring-boot-inside/demo-arthas-spring-boot/target/demo-arthas-spring-boot-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes!/logback-spring.xml

Affect(row-cnt:1) cost in 13 ms.
```
You can know the specific source of the loaded configuration.

You can try to load files that are prone to conflict:

`classloader -c <classLoaderHash> -r logback.xml`

`classloader -c <classLoaderHash> -r log4j.properties`


