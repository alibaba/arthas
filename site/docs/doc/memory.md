# memory

查看 JVM 内存信息。

具体字段信息，参考：

- [MemoryMXBean#getHeapMemoryUsage()](<https://docs.oracle.com/en/java/javase/11/docs/api/java.management/java/lang/management/MemoryMXBean.html#getHeapMemoryUsage()>)
- [MemoryMXBean#getNonHeapMemoryUsage()](<https://docs.oracle.com/en/java/javase/11/docs/api/java.management/java/lang/management/MemoryMXBean.html#getHeapMemoryUsage()>)

具体代码：

- https://github.com/alibaba/arthas/blob/master/core/src/main/java/com/taobao/arthas/core/command/monitor200/MemoryCommand.java

## 使用参考

```
$ memory
Memory                           used      total      max        usage
heap                             32M       256M       4096M      0.79%
g1_eden_space                    11M       68M        -1         16.18%
g1_old_gen                       17M       184M       4096M      0.43%
g1_survivor_space                4M        4M         -1         100.00%
nonheap                          35M       39M        -1         89.55%
codeheap_'non-nmethods'          1M        2M         5M         20.53%
metaspace                        26M       27M        -1         96.88%
codeheap_'profiled_nmethods'     4M        4M         117M       3.57%
compressed_class_space           2M        3M         1024M      0.29%
codeheap_'non-profiled_nmethods' 685K      2496K      120032K    0.57%
mapped                           0K        0K         -          0.00%
direct                           48M       48M        -          100.00%
```
