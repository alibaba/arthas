### How the CPU ratios are calculated? 

The cpu ratios here is similar to the thread `%CPU` of the linux command `top -H -p <pid>`. During a sampling interval, 
the ratio of the incremental cpu time of each thread in the current JVM to the sampling interval time.

> Working principle description:
* Do the first sampling, get the CPU time of all threads ( by calling `java.lang.management.ThreadMXBean#getThreadCpuTime()` and 
`sun.management.HotspotThreadMBean.getInternalThreadCpuTimes()` )
* Sleep and wait for an interval (the default is 200ms, the interval can be specified by `-i`)
* Do the second sampling, get the CPU time of all threads, compare the two sampling data, and calculate the incremental CPU time of each thread
* `Thread CPU usage ratio` = `Thread increment CPU time` / `Sampling interval time` * 100%

**Note:** this operation consumes CPU time too (`getThreadCpuTime` is time-consuming), therefore it is possible to observe Arthas's thread appears in the list. To avoid this, try to increase sample interval, for example: 5000 ms.<br/>

> Another way to view the thread cpu usage of the Java process, [show-busy-java-threads](https://github.com/oldratlee/useful-scripts/blob/master/docs/java.md#-show-busy-java-threads) can come to help. 
