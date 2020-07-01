
The `ognl` command can execute code dynamically.

### Invoke the static method

`ognl '@java.lang.System@out.println("hello ognl")'`{{execute T2}}

可以检查`Terminal 1`里的进程输出，可以发现打印出了`hello ognl`。


The `Terminal 1` will print `hello ognl`.


### Find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash
 classLoaderHash   1be6f5c3
```

### Get static fields of static classes

Get the `logger` field of the `UserController` class:

`ognl -c 1be6f5c3 @com.example.demo.arthas.user.UserController@logger`{{execute T2}}

Control the number of expansion layers of the return value with the `-x` parameter. such as:

`ognl -c 1be6f5c3 -x 2 @com.example.demo.arthas.user.UserController@logger`{{execute T2}}

### Execute multi-line expressions

Return a list:

`ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'`{{execute T2}}

```bash
$ ognl '#value1=@System@getProperty("java.home"), #value2=@System@getProperty("java.runtime.name"), {#value1, #value2}'
@ArrayList[
    @String[/Library/Java/JavaVirtualMachines/jdk1.8.0_162.jdk/Contents/Home/jre],
    @String[Java(TM) SE Runtime Environment],
]
```

### More

The `ognl` expression in Arthas is an important feature, and the `ognl` expression can be used in many commands.

For some more complicated usages, refer to:

* For special usage of OGNL, please refer to: https://github.com/alibaba/arthas/issues/71
* Official Guide to OGNL Expressions: https://commons.apache.org/proper/commons-ognl/language-guide.html
