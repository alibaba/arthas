
In this case, show how to dynamically modify the Logger Level.

### Find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash
 classLoaderHash   1be6f5c3
```

Please write down your classLoaderHash here, in the case here, it's `1be6f5c3`. It will be used in the future steps.

Note: Please replace `<classLoaderHash>` with your classLoaderHash above, then execute the commands manually in the following steps:

### Use ognl command to get the logger

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

The user can know that `UserController@logger` actually uses logback. Because `level=null`, the actual final level is from the `root` logger.

### Change the logger level of UserController

`ognl -c <classLoaderHash> '@com.example.demo.arthas.user.UserController@logger.setLevel(@ch.qos.logback.classic.Level@DEBUG)'`

Get `UserController@logger` again, the user can see that it is already `DEBUG`:

`ognl -c <classLoaderHash> '@com.example.demo.arthas.user.UserController@logger'`

```bash
$ ognl -c 1be6f5c3 '@com.example.demo.arthas.user.UserController@logger'
@Logger[
    serialVersionUID=@Long[5454405123156820674],
    FQCN=@String[ch.qos.logback.classic.Logger],
    name=@String[com.example.demo.arthas.user.UserController],
    level=@Level[DEBUG],
    effectiveLevelInt=@Integer[10000],
    parent=@Logger[Logger[com.example.demo.arthas.user]],
    childrenList=null,
    aai=null,
    additive=@Boolean[true],
    loggerContext=@LoggerContext[ch.qos.logback.classic.LoggerContext[default]],
]
```

### Change the global logger level of the logback

By getting the `root` logger, the user can modify the global logger level:

`ognl -c <classLoaderHash> '@org.slf4j.LoggerFactory@getLogger("root").setLevel(@ch.qos.logback.classic.Level@DEBUG)'`

