
类似传统的`tee`命令 用于读取标准输入的数据，并将其内容输出成文件。

tee指令会从标准输入设备读取数据，将其内容输出到标准输出设备，同时保存成文件。

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

## 命令示范

### 将sysprop命令执行结果另外存储在`/tmp/logfile`中

`sysprop | tee /tmp/logfile`{{execute T2}}

查看`/tmp/logfile`文件：

`cat /tmp/logfile`{{execute T2}}

### 将sysprop命令执行结果匹配`java`后另外追加在`/tmp/logfile`中

`sysprop | grep java | tee -a /path/to/logfile`{{execute T2}}

查看`/tmp/logfile`文件：

`cat /tmp/logfile`{{execute T2}}
