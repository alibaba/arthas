类似传统的 [tee 命令](https://arthas.aliyun.com/doc/tee.html) 用于读取标准输入的数据，并将其内容输出成文件。

tee 指令会从标准输入设备读取数据，将其内容输出到标准输出设备，同时保存成文件。

使用 `tee -h`{{execute T2}} 查看帮助信息

## 命令示范

### 将 sysprop 命令执行结果另外存储在`/tmp/logfile`中

`sysprop | tee /tmp/logfile`{{execute T2}}

查看`/tmp/logfile`文件：

`cat /tmp/logfile`{{execute T2}}

### 将 sysprop 命令执行结果匹配`java`后另外追加在`/tmp/logfile`中

`sysprop | grep java | tee -a /path/to/logfile`{{execute T2}}

查看`/tmp/logfile`文件：

`cat /tmp/logfile`{{execute T2}}
