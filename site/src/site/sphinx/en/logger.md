logger
===

> Print the logger information, update the logger level

### Usage

#### Print the logger information

Take the following `logback.xml` as an example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="APPLICATION" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>mylog-%d{yyyy-MM-dd}.%i.txt</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>60</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%logger{35} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="APPLICATION" />
    </appender>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%-4relative [%thread] %-5level %logger{35} - %msg %n
            </pattern>
            <charset>utf8</charset>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="ASYNC" />
    </root>
</configuration>
```


The result of the `logger` command:

```bash
[arthas@2062]$ logger
 name                                   ROOT
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher$AppClassLoader@2a139a55
 classLoaderHash                        2a139a55
 level                                  INFO
 effectiveLevel                         INFO
 additivity                             true
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
 appenders                              name            CONSOLE
                                        class           ch.qos.logback.core.ConsoleAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        target          System.out
                                        name            APPLICATION
                                        class           ch.qos.logback.core.rolling.RollingFileAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        file            app.log
                                        name            ASYNC
                                        class           ch.qos.logback.classic.AsyncAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        appenderRef     [APPLICATION]
```

In the `appenders` section:

* The target of `CONSOLE` logger is `System.out`
* `APPLICATION` logger is `RollingFileAppender`, the file is `app.log`
* `ASYNC` its `appenderRef` is `APPLICATION`, which means asynchronous output to the file

#### View logger information for the special name

```bash
[arthas@2062]$ logger -n org.springframework.web
 name                                   org.springframework.web
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher$AppClassLoader@2a139a55
 classLoaderHash                        2a139a55
 level                                  null
 effectiveLevel                         INFO
 additivity                             true
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
```

#### View logger information for the special classloader

```bash
[arthas@2062]$ logger -c 2a139a55
 name                                   ROOT
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher$AppClassLoader@2a139a55
 classLoaderHash                        2a139a55
 level                                  DEBUG
 effectiveLevel                         DEBUG
 additivity                             true
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
 appenders                              name            CONSOLE
                                        class           ch.qos.logback.core.ConsoleAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        target          System.out
                                        name            APPLICATION
                                        class           ch.qos.logback.core.rolling.RollingFileAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        file            app.log
                                        name            ASYNC
                                        class           ch.qos.logback.classic.AsyncAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        appenderRef     [APPLICATION]
```

#### Update logger level

```bash
[arthas@2062]$ logger --name ROOT --level debug
update logger level success.
```

#### 指定classloader更新 logger level

默认情况下，logger命令会在SystemClassloader下执行，如果应用是传统的`war`应用，或者spring boot fat jar启动的应用，那么需要指定classloader。

可以先用 `sc -d yourClassName` 来查看具体的 classloader hashcode，然后在更新level时指定classloader：

```bash
[arthas@2062]$ logger -c 2a139a55 --name ROOT --level debug
```

#### View the logger information without appenders


By default, the `logger` command only prints information about the logger with appenders. If you want to see information about loggers without `appender`, you can use the parameter `--include-no-appender`.

Note that the output will usually be very long.

```bash
[arthas@2062]$ logger --include-no-appender
 name                                   ROOT
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher$AppClassLoader@2a139a55
 classLoaderHash                        2a139a55
 level                                  DEBUG
 effectiveLevel                         DEBUG
 additivity                             true
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
 appenders                              name            CONSOLE
                                        class           ch.qos.logback.core.ConsoleAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        target          System.out
                                        name            APPLICATION
                                        class           ch.qos.logback.core.rolling.RollingFileAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        file            app.log
                                        name            ASYNC
                                        class           ch.qos.logback.classic.AsyncAppender
                                        classLoader     sun.misc.Launcher$AppClassLoader@2a139a55
                                        classLoaderHash 2a139a55
                                        appenderRef     [APPLICATION]

 name                                   com
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher$AppClassLoader@2a139a55
 classLoaderHash                        2a139a55
 level                                  null
 effectiveLevel                         DEBUG
 additivity                             true
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar

 name                                   com.alibaba
 class                                  ch.qos.logback.classic.Logger
 classLoader                            sun.misc.Launcher$AppClassLoader@2a139a55
 classLoaderHash                        2a139a55
 level                                  null
 effectiveLevel                         DEBUG
 additivity                             true
 codeSource                             file:/Users/hengyunabc/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar
...
```
