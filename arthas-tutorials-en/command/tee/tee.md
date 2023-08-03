Similar to the traditional [tee command](https://arthas.aliyun.com/en/doc/tee.html), it is used to read standard input data and output its contents into a file.

`tee` will read data from standard input device, output its content to standard output device, and save it as a file.

Use `tee -h`{{execute T2}} to view the tee documentation.

## Examples

### Store sysprop execution result into `/tmp/logfile`

`sysprop | tee /tmp/logfile`{{execute T2}}

Check `/tmp/logfile`:

`cat /tmp/logfile`{{execute T2}}

### Append sysprop matching `java` result into `/tmp/logfile`

`sysprop | grep java | tee -a /path/to/logfile`{{execute T2}}

Check `/tmp/logfile`:

`cat /tmp/logfile`{{execute T2}}
