
Similar to the traditional `grep` command.

`grep -h`{{execute T2}}

```bash
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

## Example

### Print matched lines

`sysprop | grep java`{{execute T2}}

### `-n` to show line numbers

`sysprop | grep java -n`{{execute T2}}

### `-v` to show non-matching lines

`sysenv | grep -v JAVA`{{execute T2}}

### `-e` to enable regular expression to match，`-m` stop after the specified number of selected lines

`sysenv | grep -e "(?i)(JAVA|sun)" -m 3  -C 2`{{execute T2}}

`thread | grep -m 10 -e  "TIMED_WAITING|WAITING"`{{execute T2}}

### `-A` to print specified line number of trailing context，`-B` to print specified line number of leading context

`sysenv | grep JAVA -A2 -B3`{{execute T2}}
