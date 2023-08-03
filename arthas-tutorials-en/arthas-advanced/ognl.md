The [ognl command](https://arthas.aliyun.com/en/doc/ognl.html) can execute code dynamically.

### Invoke the static method

`ognl '@java.lang.System@out.println("hello ognl")'`{{execute T2}}

### Find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

if you use`-c`, you have to manually type hashcode by `-c <hashcode>`.  
For classloader with only one instance, it can be specified by `--classLoaderClass` using class name, which is more convenient to use.  
The value of `--classloaderclass` is the class name of classloader. It can only work when it matches a unique classloader instance. The purpose is to facilitate the input of general commands. However, `-c <hashcode>` is dynamic.

### Get static fields of static classes

Get the `logger` field of the `UserController` class:

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader @com.example.demo.arthas.user.UserController@logger`{{execute T2}}

Control the number of expansion layers of the return value with the `-x` parameter. such as:

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -x 2 @com.example.demo.arthas.user.UserController@logger`{{execute T2}}

### Execute multi-line expressions

Return a list:

`ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'`{{execute T2}}

### More

The `ognl` expression in Arthas is an important feature, and the `ognl` expression can be used in many commands.

For some more complicated usages, refer to:

- For special usage of OGNL, please refer to: https://github.com/alibaba/arthas/issues/71
- Official Guide to OGNL Expressions: https://commons.apache.org/proper/commons-ognl/language-guide.html
