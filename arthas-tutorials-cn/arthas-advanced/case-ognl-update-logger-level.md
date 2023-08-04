在这个案例里，动态修改应用的 Logger Level。

## 使用 [sc](https://arthas.aliyun.com/doc/sc.html) 查找 UserController 的 ClassLoader

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。  
如果你使用`-c`，你需要手动输入 hashcode：`-c <hashcode>`  
对于只有唯一实例的 ClassLoader 可以通过`--classLoaderClass`指定 class name，使用起来更加方便：  
`--classLoaderClass` 的值是 ClassLoader 的类名，只有匹配到唯一的 ClassLoader 实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。

## 使用 [ognl](https://arthas.aliyun.com/doc/ognl.html#ognl)

### 用 ognl 获取 logger

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

可以知道 `UserController@logger` 实际使用的是 logback。可以看到 `level=null` ，则说明实际最终的 level 是从 `root` logger 里来的。

### 单独设置 UserController 的 logger level

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger.setLevel(@ch.qos.logback.classic.Level@DEBUG)'`{{execute T2}}

再次获取 `UserController@logger`，可以发现已经是 `DEBUG` 了：

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

### 修改 logback 的全局 logger level

通过获取 `root` logger，可以修改全局的 logger level：

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@org.slf4j.LoggerFactory@getLogger("root").setLevel(@ch.qos.logback.classic.Level@DEBUG)'`{{execute T2}}

## 使用 [logger](https://arthas.aliyun.com/doc/logger.html)

使用 `logger` 命令可以相较于 `ognl` 更加便捷的实现 logger level 的动态设置。

## 使用 logger 命令获取对应 logger 信息

`logger --name com.example.demo.arthas.user.UserController`{{exec}}

### 单独设置 UserController 的 logger level

`logger --name com.example.demo.arthas.user.UserController --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --level WARN`{{exec}}

再次获取对应 logger 信息，可以发现已经是 `WARN` 了：

`logger --name com.example.demo.arthas.user.UserController`{{exec}}

### 修改 logback 的全局 logger level

`logger --name ROOT --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --level WARN`{{exec}}
