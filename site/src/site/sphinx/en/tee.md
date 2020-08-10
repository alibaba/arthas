tee
===

[`tee` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-tee)

> Similar to the traditional `tee` command, it is used to read standard input data and output its contents into a file.

> `tee` will read data from standard input device, output its content to standard output device, and save it as a file.


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