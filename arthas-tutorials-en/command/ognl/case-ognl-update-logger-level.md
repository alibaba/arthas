In this case, show how to dynamically modify the Logger Level.

### Find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

### Use ognl command to get the logger

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

The user can know that `UserController@logger` actually uses logback. Because `level=null`, the actual final level is from the `root` logger.

### Change the logger level of UserController

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger.setLevel(@ch.qos.logback.classic.Level@DEBUG)'`{{execute T2}}

Get `UserController@logger` again, the user can see that it is already `DEBUG`:

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

### Change the global logger level of the logback

By getting the `root` logger, the user can modify the global logger level:

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@org.slf4j.LoggerFactory@getLogger("root").setLevel(@ch.qos.logback.classic.Level@DEBUG)'`{{execute T2}}
