
The `ognl` command can execute code dynamically.

Check the usage: `ognl --help`{{execute T2}}

### Invoke the static method

`ognl '@java.lang.System@out.println("hello ognl")'`{{execute T2}}

You can check the output of the process in `Terminal 1`, and find that it displays `hello ognl`.

### Find the ClassLoader of the UserController

`sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash`{{execute T2}}

```bash
$ sc -d com.example.demo.arthas.user.UserController | grep classLoaderHash
 classLoaderHash   1be6f5c3
```

Please write down your classLoaderHash here, in the case here, it's `1be6f5c3`. It will be used in the future steps.

Note: Please replace `<classLoaderHash>` with your classLoaderHash above, then execute the commands manually in the following steps:

### Get static fields of static classes

Get the `logger` field of the `UserController` class:

`ognl -c <classLoaderHash> @com.example.demo.arthas.user.UserController@logger`

Control the number of expansion layers of the return value with the `-x` parameter. such as:

`ognl -c <classLoaderHash> -x 2 @com.example.demo.arthas.user.UserController@logger`

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
