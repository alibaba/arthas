
这里的cpu统计的是，一段采样间隔内，当前JVM里各个线程所占用的cpu时间占总cpu时间的百分比。其计算方法为： 首先进行一次采样，获得所有线程的cpu的使用时间(调用的是`java.lang.management.ThreadMXBean#getThreadCpuTime`这个接口)，然后睡眠一段时间，默认100ms，可以通过`-i`参数指定，然后再采样一次，最后得出这段时间内各个线程消耗的cpu时间情况，最后算出百分比。

注意： 这个统计也会产生一定的开销（JDK这个接口本身开销比较大），因此会看到as的线程占用一定的百分比，为了降低统计自身的开销带来的影响，可以把采样间隔拉长一些，比如5000毫秒。

如果想看从Java进程启动开始到现在的cpu占比情况：可以使用[`show-busy-java-threads`](https://github.com/oldratlee/useful-scripts/blob/master/docs/java.md#-show-busy-java-threads)这个脚本
