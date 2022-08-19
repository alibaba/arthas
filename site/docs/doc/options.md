# options

[`options`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-options)

::: tip
全局开关
:::

| 名称                   | 默认值 | 描述                                                                                                                                                       |
| ---------------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| unsafe                 | false  | 是否支持对系统级别的类进行增强，打开该开关可能导致把 JVM 搞挂，请慎重选择！                                                                                |
| dump                   | false  | 是否支持被增强了的类 dump 到外部文件中，如果打开开关，class 文件会被 dump 到`/${application working dir}/arthas-class-dump/`目录下，具体位置详见控制台输出 |
| batch-re-transform     | true   | 是否支持批量对匹配到的类执行 retransform 操作                                                                                                              |
| json-format            | false  | 是否支持 json 化的输出                                                                                                                                     |
| disable-sub-class      | false  | 是否禁用子类匹配，默认在匹配目标类的时候会默认匹配到其子类，如果想精确匹配，可以关闭此开关                                                                 |
| support-default-method | true   | 是否支持匹配到 default method， 默认会查找 interface，匹配里面的 default method。参考 [#1105](https://github.com/alibaba/arthas/issues/1105)               |
| save-result            | false  | 是否打开执行结果存日志功能，打开之后所有命令的运行结果都将保存到`~/logs/arthas-cache/result.log`中                                                         |
| job-timeout            | 1d     | 异步后台任务的默认超时时间，超过这个时间，任务自动停止；比如设置 1d, 2h, 3m, 25s，分别代表天、小时、分、秒                                                 |
| print-parent-fields    | true   | 是否打印在 parent class 里的 filed                                                                                                                         |
| verbose                | false  | 是否打印更多详细信息                                                                                                                                       |
| strict                 | true   | 是否启用 strict 模式                                                                                                                                       |

## 查看所有的 options

```bash
$ options
 LEVEL  TYPE    NAME          VALUE   SUMMARY               DESCRIPTION
-------------------------------------------------------------------------------------------------------
 0      boolea  unsafe        false   Option to support sy  This option enables to proxy functionality
        n                             stem-level class       of JVM classes. Due to serious security r
                                                            isk a JVM crash is possibly be introduced.
                                                             Do not activate it unless you are able to
                                                             manage.
 1      boolea  dump          false   Option to dump the e  This option enables the enhanced classes t
        n                             nhanced classes       o be dumped to external file for further d
                                                            e-compilation and analysis.
 1      boolea  batch-re-tra  true    Option to support ba  This options enables to reTransform classe
        n       nsform                tch reTransform Clas  s with batch mode.
                                      s
 2      boolea  json-format   false   Option to support JS  This option enables to format object outpu
        n                             ON format of object   t with JSON when -x option selected.
                                      output
 1      boolea  disable-sub-  false   Option to control in  This option disable to include sub class w
        n       class                 clude sub class when  hen matching class.
                                       class matching
 1      boolea  support-defa  true    Option to control in  This option disable to include default met
        n       ult-method            clude default method  hod in interface when matching class.
                                       in interface when c
                                      lass matching
 1      boolea  save-result   false   Option to print comm  This option enables to save each command's
        n                             and's result to log    result to log file, which path is ${user.
                                      file                  home}/logs/arthas-cache/result.log.
 2      String  job-timeout   1d      Option to job timeou  This option setting job timeout,The unit c
                                      t                     an be d, h, m, s for day, hour, minute, se
                                                            cond. 1d is one day in default
 1      boolea  print-parent  true    Option to print all   This option enables print files in parent
        n       -fields               fileds in parent cla  class, default value true.
                                      ss
 1      boolea  verbose       false   Option to print verb  This option enables print verbose informat
        n                             ose information       ion, default value false.
 1      boolea  strict        true    Option to strict mod  By default, strict mode is true, not allow
        n                             e                     ed to set object properties. Want to set o
                                                            bject properties, execute `options strict
                                                            false`
```

## 获取 option 的值

```
$ options json-format
 LEVEL  TYPE  NAME         VALUE  SUMMARY             DESCRIPTION
--------------------------------------------------------------------------------------------
 2      bool  json-format  false  Option to support   This option enables to format object
        ean                       JSON format of obj  output with JSON when -x option selec
                                  ect output          ted.
```

::: tip
默认情况下`json-format`为 false，如果希望`watch`/`tt`等命令结果以 json 格式输出，则可以设置`json-format`为 true。
:::

## 设置指定的 option

例如，想打开执行结果存日志功能，输入如下命令即可：

```
$ options save-result true
 NAME         BEFORE-VALUE  AFTER-VALUE
----------------------------------------
 save-result  false         true
```

## 打开 unsafe 开关，支持 jdk package 下的类

默认情况下，`watch`/`trace`/`tt`/`trace`/`monitor`等命令不支持`java.*` package 下的类。可以设置`unsafe`为 true，则可以增强。

```bash
$ options unsafe true
 NAME    BEFORE-VALUE  AFTER-VALUE
-----------------------------------
 unsafe  false         true
```

```bash
$ watch java.lang.invoke.Invokers callSiteForm
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 61 ms, listenerId: 1
```

## 关闭 strict 模式，允许在 ognl 表达式里设置对象属性

::: tip
since 3.6.0
:::

对于新用户，在编写 ognl 表达式时，可能会出现误用。

比如对于`Student`，判断年龄等于 18 时，可能条件表达式会误写为`target.age=18`，这个表达式实际上是把当前对象的`age`设置为 18 了。正确的写法是`target.age==18`。

为了防止出现类似上面的误用，Arthas 默认启用`strict`模式，在`ognl`表达式里，禁止更新对象的 Property 或者调用`setter`函数。

以`MathGame`为例，会出现以下的错误提示。

```
$ watch demo.MathGame primeFactors 'target' 'target.illegalArgumentCount=1'
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 206 ms, listenerId: 1
watch failed, condition is: target.illegalArgumentCount=1, express is: target, By default, strict mode is true, not allowed to set object properties. Want to set object properties, execute `options strict false`, visit /Users/admin/logs/arthas/arthas.log for more details.
```

用户如果确定要在`ognl`表达式里更新对象，可以执行`options strict false`，关闭`strict`模式。

- 更多信息参考： https://github.com/alibaba/arthas/issues/2128
