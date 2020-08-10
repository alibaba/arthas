grep
===

[`grep`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-grep)

> 类似传统的`grep`命令。


```
 USAGE:
   grep [-A <value>] [-B <value>] [-C <value>] [-h] [-i] [-v] [-n] [-m <value>] [-e] [--trim-end] pattern

 SUMMARY:
   grep command for pipes.

 EXAMPLES:
  sysprop | grep java
  sysprop | grep java -n
  sysenv | grep -v JAVA
  sysenv | grep -e "(?i)(JAVA|sun)" -m 3  -C 2
  sysenv | grep JAVA -A2 -B3
  thread | grep -m 10 -e  "TIMED_WAITING|WAITING"

 WIKI:
   https://arthas.aliyun.com/doc/grep

 OPTIONS:
 -A, --after-context <value>                                                    Print NUM lines of trailing context)
 -B, --before-context <value>                                                   Print NUM lines of leading context)
 -C, --context <value>                                                          Print NUM lines of output context)
 -h, --help                                                                     this help
 -i, --ignore-case                                                              Perform case insensitive matching.  By default, grep is case sensitive.
 -v, --invert-match                                                             Select non-matching lines
 -n, --line-number                                                              Print line number with output lines
 -m, --max-count <value>                                                        stop after NUM selected lines)
 -e, --regex                                                                    Enable regular expression to match
     --trim-end                                                                 Remove whitespaces at the end of the line
 <pattern>                                                                      Pattern
```