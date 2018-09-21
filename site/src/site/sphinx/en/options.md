options
===

> Global options

| Name                | Default Value   | Description                                       |
| ------------------ | ----- | ---------------------------------------- |
| unsafe             | false | Enable support for system-level class enhancement; JVM might crash, if you turn on this switch (please use with great caution!)   |
| dump               | false | Enable support for dumping enhanced class to external file，if turned on，class file will be dumped to`/${application dir}/arthas-class-dump/`，please see console for specific location |
| batch-re-transform | true  | re-transform matched classes in batch            |
| json-format        | false | Enable output in JSON format                             |
| disable-sub-class  | false | Disabling child class matching，by default child class will be matched during matching target class，if you wish exact matching，you can turn this off |
| debug-for-asm      | false | Print ASM related debug message                             |
| save-result        | false | Enable saving logs for task results，when turn to true, all command results will be saved to `/home/admin/logs/arthas/arthas.log` |
| job-timeout        | 1d    | Default time-out time for back-stage tasks，if exceed this time，task will be stopped；i.e. 1d, 2h, 3m, 25s，representing day、hour、minute、second |

### Usage

For example，if you wish to save logs for command results, you can use following command: 

```
$ options save-result true                                                                                         
 NAME         BEFORE-VALUE  AFTER-VALUE                                                                            
----------------------------------------                                                                           
 save-result  false         true
```
