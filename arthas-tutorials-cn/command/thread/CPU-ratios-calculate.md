### cpu 使用率是如何统计出来的？

这里的 cpu 使用率与 linux 命令`top -H -p <pid>` 的线程`%CPU`类似，一段采样间隔时间内，当前 JVM 里各个线程的增量 cpu 时间与采样间隔时间的比例。

> 工作原理说明：

- 首先第一次采样，获取所有线程的 CPU 时间 (调用的是`java.lang.management.ThreadMXBean#getThreadCpuTime()`及`sun.management.HotspotThreadMBean.getInternalThreadCpuTimes()`接口)
- 然后睡眠等待一个间隔时间（默认为 200ms，可以通过`-i`指定间隔时间）
- 再次第二次采样，获取所有线程的 CPU 时间，对比两次采样数据，计算出每个线程的增量 CPU 时间
- 线程 CPU 使用率 = 线程增量 CPU 时间 / 采样间隔时间 \* 100%

> 注意：这个统计也会产生一定的开销（JDK 这个接口本身开销比较大），因此会看到 as 的线程占用一定的百分比，为了降低统计自身的开销带来的影响，可以把采样间隔拉长一些，比如 5000 毫秒。

> 另外一种查看 Java 进程的线程 cpu 使用率方法：可以使用[`show-busy-java-threads`](https://github.com/oldratlee/useful-scripts/blob/dev-2.x/docs/java.md#-show-busy-java-threads)这个脚本。
