在这个案例里，展示排查 logger 冲突的方法。

### 确认应用使用的 logger 系统

以`UserController`为例，它使用的是 slf4j api，但实际使用到的 logger 系统是 logback。

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

### 获取 logback 实际加载的配置文件

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '#map1=@org.slf4j.LoggerFactory@getLogger("root").loggerContext.objectMap, #map1.get("CONFIGURATION_WATCH_LIST")'`{{execute T2}}

### 使用 classloader 命令查找可能存在的 logger 配置文件

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml`{{execute T2}}

可以知道加载的配置的具体来源。

可以尝试加载容易冲突的文件：

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback.xml`{{execute T2}}

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r log4j.properties`{{execute T2}}
