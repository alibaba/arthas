# heapdump

[`heapdump`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-heapdump)

::: tip
dump java heap, 类似 jmap 命令的 heap dump 功能。
:::

## 使用参考

### dump 到指定文件

```bash
[arthas@58205]$ heapdump arthas-output/dump.hprof
Dumping heap to arthas-output/dump.hprof ...
Heap dump file created
```

::: tip
生成文件在`arthas-output`目录，可以通过浏览器下载： http://localhost:8563/arthas-output/
:::

### 只 dump live 对象

```bash
[arthas@58205]$ heapdump --live /tmp/dump.hprof
Dumping heap to /tmp/dump.hprof ...
Heap dump file created
```

## dump 到临时文件

```bash
[arthas@58205]$ heapdump
Dumping heap to /var/folders/my/wy7c9w9j5732xbkcyt1mb4g40000gp/T/heapdump2019-09-03-16-385121018449645518991.hprof...
Heap dump file created
```
