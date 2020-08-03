
CPU ratio for a given thread is the CPU time it takes divided by the total CPU time within a specified interval period. It is calculated in the following way: sample CPU times for all the thread by calling `java.lang.management.ThreadMXBean#getThreadCpuTime` first, then sleep for a period (the default value is 100ms, which can be specified by `-i`), then sample CPU times again. By this, we can get the time cost for this period for each thread, then come up with the ratio.

**Note**: this operation consumes CPU time too (getThreadCpuTime is time-consuming), therefore it is possible to observe Arthas’s thread appears in the list. To avoid this, try to increase sample interval, for example: 5000 ms.

If you’d like to check the CPU ratios from the very beginning of the Java process, [`show-busy-java-threads`](https://github.com/oldratlee/useful-scripts/blob/master/docs/java.md#-show-busy-java-threads) can come to help.
