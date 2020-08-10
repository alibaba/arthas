tee
===

[`tee`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-tee)

> 类似传统的`tee`命令, 用于读取标准输入的数据，并将其内容输出成文件。

> tee指令会从标准输入设备读取数据，将其内容输出到标准输出设备，同时保存成文件。


```
 USAGE:
   tee [-a] [-h] [file]

 SUMMARY:
   tee command for pipes.

 EXAMPLES:
  sysprop | tee /path/to/logfile | grep java
  sysprop | tee -a /path/to/logfile | grep java

 WIKI:
   https://arthas.aliyun.com/doc/tee

 OPTIONS:
 -a, --append                              Append to file
 -h, --help                                this help
 <file>                                    File path
```