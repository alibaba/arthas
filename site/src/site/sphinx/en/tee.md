tee
===

> Similar to the traditional `tee` command.


```
 USAGE:
   tee [-a] [-h] [file]

 SUMMARY:
   tee command for pipes.

 EXAMPLES:
  sysprop | tee /path/to/logfile | grep java
  sysprop | tee -a /path/to/logfile | grep java

 WIKI:
   https://alibaba.github.io/arthas/tee

 OPTIONS:
 -a, --append                              Append to file
 -h, --help                                this help
 <file>                                    File path
```