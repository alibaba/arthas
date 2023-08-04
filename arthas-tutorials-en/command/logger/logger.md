Print the logger information, update the logger level

[logger command Docs](https://arthas.aliyun.com/en/doc/logger.html)

### Usage

#### Print the logger information

`logger`{{execute T2}}

#### View logger information for the special name

`logger -n org.springframework.web`{{execute T2}}

Note: Please replace `<classLoaderHash>` with your classLoaderHash above, then execute the commands manually in the following steps:

#### View logger information for the special classloader

Note that the hashcode changes, you need to check the current ClassLoader information first, and extract the hashcode corresponding to the ClassLoader.  
If you are using `-c`, you will need to manually enter the hashcode obtained from `logger -n org.springframework.web | grep classLoaderHash`{{exec}} as follows: `-c <hashcode>`.  
For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.

`logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader`{{execute T2}}

The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

#### Update logger level

`logger --name ROOT --level debug`{{execute T2}}

PS: Here it will come up with an error message in tutorial, we have to specify the classloader.

#### Speecify classloader to update logger level

By default，logger command will be executed under SystemClassloader, if the application is a traditional `war`, or using spring boot fat jar, then it needs to specify classloader。

You can first use `sc -d yourClassName` to check specified classloader hashcode，then specify classloader when updating logger level:

`logger --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --name ROOT --level debug`{{execute T2}}

#### View the logger information without appenders

By default, the `logger` command only prints information about the logger with appenders. If you want to see information about loggers without `appender`, you can use the parameter `--include-no-appender`.

Note that the output will usually be very long.

`logger --include-no-appender`{{execute T2}}
