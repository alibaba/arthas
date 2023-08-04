In this case, show how to troubleshoot logger conflicts.

### Find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

Please write down your classLoaderHash here since it's dynamic. In the case here, it's `1be6f5c3`.  
if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.  
For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.  
The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

### View the logger system used by the app

Take `UserController` as an example, it uses slf4j api, but the actual logger system used is logback.

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '@com.example.demo.arthas.user.UserController@logger'`{{execute T2}}

### Find the configuration file actually loaded by the logback

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader '#map1=@org.slf4j.LoggerFactory@getLogger("root").loggerContext.objectMap, #map1.get("CONFIGURATION_WATCH_LIST")'`{{execute T2}}

### Use the classloader command to find possible logger configuration files

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback-spring.xml`{{execute T2}}

You can know the specific source of the loaded configuration.

You can try to load files that are prone to conflict:

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r logback.xml`{{execute T2}}

`classloader --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -r log4j.properties`{{execute T2}}
