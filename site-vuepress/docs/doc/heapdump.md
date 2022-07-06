heapdump
===

[`heapdump`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-heapdump)

> dump java heap, 类似jmap命令的heap dump功能。


### 使用参考

#### dump到指定文件

```bash
[arthas@58205]$ heapdump /tmp/dump.hprof
Dumping heap to /tmp/dump.hprof...
Heap dump file created
```

#### 只dump live对象

```bash
[arthas@58205]$ heapdump --live /tmp/dump.hprof
Dumping heap to /tmp/dump.hprof...
Heap dump file created
```

### dump到临时文件

```bash
[arthas@58205]$ heapdump
Dumping heap to /var/folders/my/wy7c9w9j5732xbkcyt1mb4g40000gp/T/heapdump2019-09-03-16-385121018449645518991.hprof...
Heap dump file created
```



