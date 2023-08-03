在这个案例里，动态修改应用的 Logger Level。

### 查找 UserController 的 ClassLoader

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

### 用 ognl 获取 logger

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

可以知道`UserController@logger`实际使用的是 logback。可以看到`level=null`，则说明实际最终的 level 是从`root` logger 里来的。

### 单独设置 UserController 的 logger level

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger.setLevel(@ch.qos.logback.classic.Level@DEBUG)'`{{execute T2}}

再次获取`UserController@logger`，可以发现已经是`DEBUG`了：

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

### 修改 logback 的全局 logger level

通过获取`root` logger，可以修改全局的 logger level：

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@org.slf4j.LoggerFactory@getLogger("root").setLevel(@ch.qos.logback.classic.Level@DEBUG)'`{{execute T2}}
