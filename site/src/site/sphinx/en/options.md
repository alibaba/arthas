options
===

> Global options

|Name| Default Value   |         Description             |
| ------------------------- | ----- | ---------------------------------------- |
| unsafe             | false | whether to enhance to system-level class. Use it with caution since JVM may hang|
| dump               | false | whether to dump enhanced class to the external files. If it's on, enhanced class will be dumped into `/${application dir}/arthas-class-dump/`, the specific output path will be output in the console |
| batch-re-transform | true  | whether to re-transform matched classes in batch|
| json-format        | false | whether to output in JSON format|
| disable-sub-class  | false | whether to enable matching child classes. The default value is `true`. If exact match is desire, turn off this flag|
| debug-for-asm      | false | whether to enable ASM debugging log|
| save-result        | false | whether to save execution result. All execution results will be saved to `/home/admin/logs/arthas/arthas.log` when it's turned on|
| job-timeout        | 1d    | default timeout for background jobs. Background job will be terminated once it's timed out (i.e. 1d, 2h, 3m, 25s)|

### Usage

For example, to enable saving command execution result, input the command below:

```
$ options save-result true                                                                                         
 NAME         BEFORE-VALUE  AFTER-VALUE                                                                            
----------------------------------------                                                                           
 save-result  false         true
```
