在这个案例里，展示排查 logger 冲突的方法。

### 查找 UserController 的 ClassLoader

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。  
如果你使用`-c`，你需要手动输入 hashcode：`-c <hashcode>`  
对于只有唯一实例的 ClassLoader 可以通过`--classLoaderClass`指定 class name，使用起来更加方便：  
`--classLoaderClass` 的值是 ClassLoader 的类名，只有匹配到唯一的 ClassLoader 实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。

### 确认应用使用的 logger 系统

以 `UserController` 为例，它使用的是 slf4j api，但实际使用到的 logger 系统是 logback。

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

### 获取 logback 实际加载的配置文件

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '#map1=@org.slf4j.LoggerFactory@getLogger("root").loggerContext.objectMap, #map1.get("CONFIGURATION_WATCH_LIST")'`{{execute T2}}

### 使用 classloader 命令查找可能存在的 logger 配置文件

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml`{{execute T2}}

可以知道加载的配置的具体来源。

可以尝试加载容易冲突的文件：

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback.xml`{{execute T2}}

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r log4j.properties`{{execute T2}}
