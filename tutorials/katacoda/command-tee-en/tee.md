
Similar to the traditional `tee` command, it is used to read standard input data and output its contents into a file.

`tee` will read data from standard input device, output its content to standard output device, and save it as a file.

`tee -h`{{execute T2}}

```bash
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

## Examples

### Store sysprop execution result into `/tmp/logfile`

`sysprop | tee /tmp/logfile`{{execute T2}}

Check `/tmp/logfile`:

`cat /tmp/logfile`{{execute T2}}

### Append sysprop matching `java` result into `/tmp/logfile`

`sysprop | grep java | tee -a /path/to/logfile`{{execute T2}}

Check `/tmp/logfile`:

`cat /tmp/logfile`{{execute T2}}
