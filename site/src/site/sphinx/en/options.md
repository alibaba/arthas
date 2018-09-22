options
===

> Global options

|&nbsp;&nbsp;&nbsp;Name&nbsp;&nbsp;&nbsp;| Default Value   |         Description             |
| ------------------------- | ----- | ---------------------------------------- |
| unsafe             | false | Enable system-level class enhancement; JVM might crash, if you turn it on (use with great caution :exclamation:)   |
| dump               | false | Enable support for dumping enhanced class to external file; if turned on, class file will be dumped to`/${application dir}/arthas-class-dump/`，please check console output for specific location |
| batch-re-transform | true  | re-transform matched classes in batch            |
| json-format        | false | Enable output in JSON format                             |
| disable-sub-class  | false | Disabling child class matching: by default child class will be matched while matching target class; if you wish exact matching, you should turn it off |
| debug-for-asm      | false | Print ASM-related debug message                             |
| save-result        | false | Enable saving logs for task results: when true, all command results will be saved to `/home/admin/logs/arthas/arthas.log` |
| job-timeout        | 1d    | Default timeout for background jobs: jobs will be stopped once timed out (i.e. 1d, 2h, 3m, 25s)|

### Usage

Saving logs for command outputs, you can:

```
$ options save-result true                                                                                         
 NAME         BEFORE-VALUE  AFTER-VALUE                                                                            
----------------------------------------                                                                           
 save-result  false         true
```
