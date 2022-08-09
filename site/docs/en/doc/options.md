# options

[`options` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-options)

::: tip
Global options
:::

| Name                   | Default Value | Description                                                                                                                                                                                           |
| ---------------------- | ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| unsafe                 | false         | whether to enhance to system-level class. Use it with caution since JVM may hang                                                                                                                      |
| dump                   | false         | whether to dump enhanced class to the external files. If it's on, enhanced class will be dumped into `/${application dir}/arthas-class-dump/`, the specific output path will be output in the console |
| batch-re-transform     | true          | whether to re-transform matched classes in batch                                                                                                                                                      |
| json-format            | false         | whether to output in JSON format                                                                                                                                                                      |
| disable-sub-class      | false         | whether to enable matching child classes. The default value is `true`. If exact match is desire, turn off this flag                                                                                   |
| support-default-method | true          | whether to enable matching default method in interface. The default value is `true`. Refer to [#1105](https://github.com/alibaba/arthas/issues/1105)                                                  |
| save-result            | false         | whether to save execution result. All execution results will be saved to `~/logs/arthas-cache/result.log` when it's turned on                                                                         |
| job-timeout            | 1d            | default timeout for background jobs. Background job will be terminated once it's timed out (i.e. 1d, 2h, 3m, 25s)                                                                                     |
| print-parent-fields    | true          | This option enables print files in parent class, default value true.                                                                                                                                  |
| verbose                | false         | This option enables print verbose information                                                                                                                                                         |
| strict                 | true          | whether to enable strict mode                                                                                                                                                                         |

## View all options

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

## Get special option value

```
$ options json-format
 LEVEL  TYPE  NAME         VALUE  SUMMARY             DESCRIPTION
--------------------------------------------------------------------------------------------
 2      bool  json-format  false  Option to support   This option enables to format object
        ean                       JSON format of obj  output with JSON when -x option selec
                                  ect output          ted.
```

::: tip
By default, `json-format` is false. When set `json-format` to true, commands like `wathc`/`tt` will print result with `json` format.
:::

## Set special option value

For example, to enable saving command execution result, input the command below:

```
$ options save-result true
 NAME         BEFORE-VALUE  AFTER-VALUE
----------------------------------------
 save-result  false         true
```

## Set `unsafe` to true to enhance the classes under the `java.*` package

By default, `watch`/`trace`/`tt`/`trace`/`monitor` command do not support classes under `java.*` package. You can set `unsafe` to true to enhance the classes under the `java.*` package.

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

## Turn off strict mode, allow setting object properties in ognl expressions

::: tip
since 3.6.0
:::

For new users, there may be misuses when writing ognl expressions.

For example, for `Student`, when judging the age is equal to 18, the conditional expression may be mistakenly written as `target.age=18`, which actually sets the `age` of the current object to 18. The correct spelling is `target.age==18`.

In order to prevent misuse like the above, Arthas enables `strict` mode by default, in `ognl` expressions, it is forbidden to update the property of the object or call the `setter` method.

Take `MathGame` as an example, the following error message will appear.

```
$ watch demo.MathGame primeFactors 'target' 'target.illegalArgumentCount=1'
Press Q or Ctrl+C to abort.
Affect(class count: 1 , method count: 1) cost in 206 ms, listenerId: 1
watch failed, condition is: target.illegalArgumentCount=1, express is: target, By default, strict mode is true, not allowed to set object properties. Want to set object properties, execute `options strict false`, visit /Users/admin/logs/arthas/arthas.log for more details.
```

If the user want to change the object properties in the `ognl` expression, you can execute `options strict false` to turn off the `strict` mode.

- For more information, please refer to: https://github.com/alibaba/arthas/issues/2128
