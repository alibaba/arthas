
In this case, show how to dynamically modify the Logger Level.

### Find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash
 classLoaderHash   1be6f5c3
```

Please write down your classLoaderHash here since it's dynamic. In the case here, it's `1be6f5c3`.

if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.

```bash
$ ognl -c 1be6f5c3 @com.example.demo.arthas.user.UserController@logger
```

For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

```bash
$ ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader  @org.springframework.boot.SpringApplication@logger
@Slf4jLocationAwareLog[
    FQCN=@String[org.apache.commons.logging.LogAdapter$Slf4jLocationAwareLog],
    name=@String[org.springframework.boot.SpringApplication],
    logger=@Logger[Logger[org.springframework.boot.SpringApplication]],
]
```

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

### Use ognl command to get the logger

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

The user can know that `UserController@logger` actually uses logback. Because `level=null`, the actual final level is from the `root` logger.

### Change the logger level of UserController

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger.setLevel(@ch.qos.logback.classic.Level@DEBUG)'`{{execute T2}}

Get `UserController@logger` again, the user can see that it is already `DEBUG`:

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

```bash
$ ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'
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

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@org.slf4j.LoggerFactory@getLogger("root").setLevel(@ch.qos.logback.classic.Level@DEBUG)'`{{execute T2}}

