查看 logger 信息，更新 logger level

[logger 命令文档](https://arthas.aliyun.com/doc/logger.html)

### 使用参考

#### 查看所有 logger 信息

`logger`{{execute T2}}

#### 查看指定名字的 logger 信息

`logger -n org.springframework.web`{{execute T2}}

#### 查看指定 classloader 的 logger 信息

注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。
如果你使用`-c`，你需要手动输入由 `logger -n org.springframework.web | grep classLoaderHash`{{exec}} 获取到的 hashcode：`-c <hashcode>`  
对于只有唯一实例的 ClassLoader 可以通过`--classLoaderClass`指定 class name，使用起来更加方便：

`logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader`{{execute T2}}

`--classLoaderClass` 的值是 ClassLoader 的类名，只有匹配到唯一的 ClassLoader 实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。

#### 更新 logger level

`logger --name ROOT --level debug`{{execute T2}}

注意：在教程中执行会提示错误，需要指定 classloader

#### 指定 classloader 更新 logger level

默认情况下，logger 命令会在 SystemClassloader 下执行，如果应用是传统的`war`应用，或者 spring boot fat jar 启动的应用，那么需要指定 classloader。

可以先用 `sc -d yourClassName` 来查看具体的 classloader hashcode，然后在更新 level 时指定 classloader：

`logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --name ROOT --level debug`{{execute T2}}

#### 查看没有 appender 的 logger 的信息

默认情况下，`logger`命令只打印有 appender 的 logger 的信息。如果想查看没有`appender`的 logger 的信息，可以加上参数`--include-no-appender`。

注意，通常输出结果会很长。

`logger --include-no-appender`{{execute T2}}
