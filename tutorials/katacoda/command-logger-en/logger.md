
Print the logger information, update the logger level

### Usage

#### Print the logger information

`logger`{{execute T2}}

```bash
[arthas@2062]$ logger
 name              ROOT
 class             ch.qos.logback.classic.Logger
 classLoader       org.springframework.boot.loader.LaunchedURLClassLoader@5674cd4d
 classLoaderHash   5674cd4d
 level             INFO
 effectiveLevel    INFO
 additivity        true
 codeSource        jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/logback-classi
                   c-1.1.11.jar!/
 appenders         name            CONSOLE
                   class           ch.qos.logback.core.ConsoleAppender
                   classLoader     org.springframework.boot.loader.LaunchedURLClassLoader@5674cd4d
                   classLoaderHash 5674cd4d
                   target          System.out
...
```

#### View logger information for the special name

`logger -n org.springframework.web`{{execute T2}}

```bash
[arthas@2062]$ logger -n org.springframework.web
 name              org.springframework.web
 class             ch.qos.logback.classic.Logger
 classLoader       org.springframework.boot.loader.LaunchedURLClassLoader@5674cd4d
 classLoaderHash   5674cd4d
 level             null
 effectiveLevel    INFO
 additivity        true
 codeSource        jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/logback-classi
                   c-1.1.11.jar!/
```


Please write down your classLoaderHash here, in the case here, it's `5674cd4d`. It will be used in the future steps.

Note: Please replace `<classLoaderHash>` with your classLoaderHash above, then execute the commands manually in the following steps:

#### View logger information for the special classloader

Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.

if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.

```bash
$ logger -c 5674cd4d
```

For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

`logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader`{{execute T2}}

```bash
[arthas@2062]$ logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader
name              ROOT
 class             ch.qos.logback.classic.Logger
 classLoader       org.springframework.boot.loader.LaunchedURLClassLoader@5674cd4d
 classLoaderHash   5674cd4d
 level             INFO
 effectiveLevel    INFO
 additivity        true
 codeSource        jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/logback-classi
                   c-1.1.11.jar!/
 appenders         name            CONSOLE
                   class           ch.qos.logback.core.ConsoleAppender
                   classLoader     org.springframework.boot.loader.LaunchedURLClassLoader@5674cd4d
                   classLoaderHash 5674cd4d
                   target          System.out
...
```

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

#### Update logger level

`logger --name ROOT --level debug`{{execute T2}}

```bash
[arthas@2062]$ logger --name ROOT --level debug
update logger level success.
```

PS: Here it will come up with an error message in tutorial, we have to specify the classloader. 

#### Speecify classloader to update logger level

By default，logger command will be executed under SystemClassloader, if the application is a traditional `war`, or using spring boot fat jar, then it needs to specify classloader。

You can first use `sc -d yourClassName` to check specified classloader hashcode，then specify classloader when updating logger level:

`logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --name ROOT --level debug`{{execute T2}}

```bash
[arthas@2062]$ logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --name ROOT --level debug
```

#### View the logger information without appenders


By default, the `logger` command only prints information about the logger with appenders. If you want to see information about loggers without `appender`, you can use the parameter `--include-no-appender`.

Note that the output will usually be very long.

`logger --include-no-appender`{{execute T2}}

```bash
[arthas@2062]$ logger --include-no-appender
 name              org.thymeleaf
 class             ch.qos.logback.classic.Logger
 classLoader       org.springframework.boot.loader.LaunchedURLClassLoader@5674cd4d
 classLoaderHash   5674cd4d
 level             null
 effectiveLevel    INFO
 additivity        false
 codeSource        jar:file:/home/scrapbook/tutorial/demo-arthas-spring-boot.jar!/BOOT-INF/lib/logback-classi
                   c-1.1.11.jar!/
 appenders         name            DEBUG_LEVEL_REMAPPER
                   class           org.springframework.boot.logging.logback.LevelRemappingAppender
                   classLoader     org.springframework.boot.loader.LaunchedURLClassLoader@5674cd4d
                   classLoaderHash 5674cd4d
...
```
