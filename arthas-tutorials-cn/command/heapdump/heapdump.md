> dump java heap, 类似 jmap 命令的 heap dump 功能。

[heapdump 命令文档](https://arthas.aliyun.com/doc/heapdump.html)

### 使用参考

#### dump 到指定文件

`heapdump /tmp/dump.hprof`{{execute T2}}

#### 只 dump live 对象

`heapdump --live /tmp/dump.hprof`{{execute T2}}

### dump 到临时文件

`heapdump`{{execute T2}}
