查看logger信息，更新logger level

[logger 命令文档](https://arthas.aliyun.com/doc/logger.html)

### 使用参考

#### 查看所有logger信息

`logger`{{execute T2}}

#### 查看指定名字的logger信息

`logger -n org.springframework.web`{{execute T2}}

#### 查看指定classloader的logger信息

注意hashcode是变化的，需要先查看当前的ClassLoader信息，提取对应ClassLoader的hashcode。
如果你使用`-c`，你需要手动输入hashcode：`-c <hashcode>`  
对于只有唯一实例的ClassLoader可以通过`--classLoaderClass`指定class name，使用起来更加方便：

`logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader`{{execute T2}}

`--classLoaderClass` 的值是ClassLoader的类名，只有匹配到唯一的ClassLoader实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。

#### 更新logger level

`logger --name ROOT --level debug`{{execute T2}}

注意：在教程中执行会提示错误，需要指定 classloader

#### 指定classloader更新 logger level

默认情况下，logger命令会在SystemClassloader下执行，如果应用是传统的`war`应用，或者spring boot fat jar启动的应用，那么需要指定classloader。

可以先用 `sc -d yourClassName` 来查看具体的 classloader hashcode，然后在更新level时指定classloader：

`logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --name ROOT --level debug`{{execute T2}}

#### 查看没有appender的logger的信息

默认情况下，`logger`命令只打印有appender的logger的信息。如果想查看没有`appender`的logger的信息，可以加上参数`--include-no-appender`。

注意，通常输出结果会很长。

`logger --include-no-appender`{{execute T2}}
