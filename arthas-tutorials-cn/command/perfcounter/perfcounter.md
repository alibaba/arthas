查看当前 JVM 的 Perf Counter 信息

`perfcounter -h`{{execute T2}}

[perfcounter 命令文档](https://arthas.aliyun.com/doc/perfcounter.html)

## 使用参考

`perfcounter`{{execute T2}}

可以用-d 参数打印更多信息：

`perfcounter -d`{{execute T2}}

## 备注：对于 jdk9 以上的应用

如果没有打印出信息，应用在启动时，加下面的参数：

`--add-opens java.base/jdk.internal.perf=ALL-UNNAMED --add-exports java.base/jdk.internal.perf=ALL-UNNAMED --add-opens java.management/sun.management.counter.perf=ALL-UNNAMED --add-opens java.management/sun.management.counter=ALL-UNNAMED`
