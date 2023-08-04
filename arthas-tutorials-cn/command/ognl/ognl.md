在 Arthas 里，有一个单独的 [ognl 命令](https://arthas.aliyun.com/doc/ognl.html)，可以动态执行代码。

查看用法：`ognl --help`{{execute T2}}

### 调用 static 函数

`ognl '@java.lang.System@out.println("hello ognl")'`{{execute T2}}

可以检查`Terminal 1`（不是 arthas 的 Terminal 2）里的应用进程的输出，可以发现打印出了`hello ognl`。

### 查找 UserController 的 ClassLoader

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

注意 hashcode 是变化的，需要先查看当前的 ClassLoader 信息，提取对应 ClassLoader 的 hashcode。  
如果你使用`-c`，你需要手动输入 hashcode：`-c <hashcode>`  
对于只有唯一实例的 ClassLoader 可以通过`--classLoaderClass`指定 class name，使用起来更加方便：  
`--classLoaderClass` 的值是 ClassLoader 的类名，只有匹配到唯一的 ClassLoader 实例时才能工作，目的是方便输入通用命令，而`-c <hashcode>`是动态变化的。

### 获取静态类的静态字段

获取`UserController`类里的`logger`字段：

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader @com.example.demo.arthas.user.UserController@logger`{{execute T2}}

还可以通过`-x`参数控制返回值的展开层数。比如：

`ognl --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader -x 2 @com.example.demo.arthas.user.UserController@logger`{{execute T2}}

### 执行多行表达式，赋值给临时变量，返回一个 List

`ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'`{{execute T2}}

### 更多

在 Arthas 里`ognl`表达式是很重要的功能，在很多命令里都可以使用`ognl`表达式。

一些更复杂的用法，可以参考：

- OGNL 特殊用法请参考：https://github.com/alibaba/arthas/issues/71
- OGNL 表达式官方指南：https://commons.apache.org/proper/commons-ognl/language-guide.html
