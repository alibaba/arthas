Similar to the traditional [grep command](https://arthas.aliyun.com/en/doc/grep.html).

`grep -h`{{execute T2}}

## Example

### Print matched lines

`sysprop | grep java`{{execute T2}}

### `-n` to show line numbers

`sysprop | grep java -n`{{execute T2}}

### `-v` to show non-matching lines

`sysenv | grep -v JAVA`{{execute T2}}

### `-e` to enable regular expression to match,`-m` stop after the specified number of selected lines

`sysenv | grep -e "(?i)(JAVA|sun)" -m 3 -C 2`{{execute T2}}

`thread | grep -m 10 -e "TIMED_WAITING|WAITING"`{{execute T2}}

### `-A` to print specified line number of trailing context,`-B` to print specified line number of leading context

`sysenv | grep JAVA -A2 -B3`{{execute T2}}
