options
===

[`options` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-options)

> Global options

|Name| Default Value   |         Description             |
| ------------------------- | ----- | ---------------------------------------- |
| unsafe             | false | whether to enhance to system-level class. Use it with caution since JVM may hang|
| dump               | false | whether to dump enhanced class to the external files. If it's on, enhanced class will be dumped into `/${application dir}/arthas-class-dump/`, the specific output path will be output in the console |
| batch-re-transform | true  | whether to re-transform matched classes in batch|
| json-format        | false | whether to output in JSON format|
| disable-sub-class  | false | whether to enable matching child classes. The default value is `true`. If exact match is desire, turn off this flag|
| support-default-method  | true | whether to enable matching default method in interface. The default value is `true`. Refer to [#1105](https://github.com/alibaba/arthas/issues/1105) |
| save-result        | false | whether to save execution result. All execution results will be saved to `~/logs/arthas-cache/result.log` when it's turned on|
| job-timeout        | 1d    | default timeout for background jobs. Background job will be terminated once it's timed out (i.e. 1d, 2h, 3m, 25s)| print-parent-fields        | true    | This option enables print files in parent class, default value true.|



### View all options

```bash
$ options
 LEVEL  TYPE  NAME         VALUE  SUMMARY             DESCRIPTION
--------------------------------------------------------------------------------------------
 0      bool  unsafe       false  Option to support   This option enables to proxy function
        ean                       system-level class  ality of JVM classes. Due to serious
                                                      security risk a JVM crash is possibly
                                                       be introduced. Do not activate it un
                                                      less you are able to manage.
 1      bool  dump         false  Option to dump the  This option enables the enhanced clas
        ean                        enhanced classes   ses to be dumped to external file for
                                                       further de-compilation and analysis.
 1      bool  batch-re-tr  true   Option to support   This options enables to reTransform c
        ean   ansform             batch reTransform   lasses with batch mode.
                                  Class
 2      bool  json-format  false  Option to support   This option enables to format object
        ean                       JSON format of obj  output with JSON when -x option selec
                                  ect output          ted.
 1      bool  disable-sub  false  Option to control   This option disable to include sub cl
        ean   -class              include sub class   ass when matching class.
                                  when class matchin
                                  g
 1      bool  debug-for-a  false  Option to print DE  This option enables to print DEBUG me
        ean   sm                  BUG message if ASM  ssage of ASM for each method invocati
                                   is involved        on.
 1      bool  save-result  false  Option to print co  This option enables to save each comm
        ean                       mmand's result to   and's result to log file, which path
                                  log file            is ${user.home}/logs/arthas-cache/res
                                                      ult.log.
 2      Stri  job-timeout  1d     Option to job time  This option setting job timeout,The u
        ng                        out                 nit can be d, h, m, s for day, hour,
                                                      minute, second. 1d is one day in defa
                                                      ult
 1      bool  print-paren  true   Option to print al  This option enables print files in pa
        ean   t-fields            l fileds in parent  rent class, default value true.
                                   class
```


### Get special option value


```
$ options json-format
 LEVEL  TYPE  NAME         VALUE  SUMMARY             DESCRIPTION
--------------------------------------------------------------------------------------------
 2      bool  json-format  false  Option to support   This option enables to format object
        ean                       JSON format of obj  output with JSON when -x option selec
                                  ect output          ted.
```

> By default, `json-format` is false. When set `json-format` to true, commands like `wathc`/`tt` will print result with `json` format. 

### Set special option value

For example, to enable saving command execution result, input the command below:

```
$ options save-result true                                                                                         
 NAME         BEFORE-VALUE  AFTER-VALUE                                                                            
----------------------------------------                                                                           
 save-result  false         true
```

### Set `unsafe` to true to enhance the classes under the `java.*` package

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