In this case, show how to dynamically modify the Logger Level.

## Use [sc](https://arthas.aliyun.com/en/doc/sc.html) to find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.  
For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.  
The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

## Use [ognl](https://arthas.aliyun.com/en/doc/ognl.html)

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

### Use [logger](https://arthas.aliyun.com/en/doc/logger.html)

The `logger ` command provides a more convenient way to dynamically set the logger level compared to using `ognl`.

### Use logger command to get the logger

`logger --name com.example.demo.arthas.user.UserController`{{exec}}

### Change the logger level of UserController

`logger --name com.example.demo.arthas.user.UserController --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --level WARN`{{exec}}

Get the information of logger again, the user can see that it is already `WARN`:

`logger --name com.example.demo.arthas.user.UserController`{{exec}}

### Change the global logger level of the logback

`logger --name ROOT --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --level WARN`{{exec}}
