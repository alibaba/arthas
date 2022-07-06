dashboard
===

[`dashboard`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-dashboard)

> 当前系统的实时数据面板，按 ctrl+c 退出。

当运行在Ali-tomcat时，会显示当前tomcat的实时信息，如HTTP请求的qps, rt, 错误数, 线程池信息等等。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|[i:]|刷新实时数据的时间间隔 (ms)，默认5000ms|
|[n:]|刷新实时数据的次数|

### 使用参考

```
$ dashboard
ID   NAME                           GROUP           PRIORITY   STATE     %CPU      DELTA_TIME TIME      INTERRUPTE DAEMON
-1   C2 CompilerThread0             -               -1         -         1.55      0.077      0:8.684   false      true
53   Timer-for-arthas-dashboard-07b system          5          RUNNABLE  0.08      0.004      0:0.004   false      true
22   scheduling-1                   main            5          TIMED_WAI 0.06      0.003      0:0.287   false      false
-1   C1 CompilerThread0             -               -1         -         0.06      0.003      0:2.171   false      true
-1   VM Periodic Task Thread        -               -1         -         0.03      0.001      0:0.092   false      true
49   arthas-NettyHttpTelnetBootstra system          5          RUNNABLE  0.02      0.001      0:0.156   false      true
16   Catalina-utility-1             main            1          TIMED_WAI 0.0       0.000      0:0.029   false      false
-1   G1 Young RemSet Sampling       -               -1         -         0.0       0.000      0:0.019   false      true
17   Catalina-utility-2             main            1          WAITING   0.0       0.000      0:0.025   false      false
34   http-nio-8080-ClientPoller     main            5          RUNNABLE  0.0       0.000      0:0.016   false      true
23   http-nio-8080-BlockPoller      main            5          RUNNABLE  0.0       0.000      0:0.011   false      true
-1   VM Thread                      -               -1         -         0.0       0.000      0:0.032   false      true
-1   Service Thread                 -               -1         -         0.0       0.000      0:0.006   false      true
-1   GC Thread#5                    -               -1         -         0.0       0.000      0:0.043   false      true
Memory                     used     total    max      usage    GC
heap                       36M      70M      4096M    0.90%    gc.g1_young_generation.count   12
g1_eden_space              6M       18M      -1       33.33%                                  86
g1_old_gen                 30M      50M      4096M    0.74%    gc.g1_old_generation.count     0
g1_survivor_space          491K     2048K    -1       24.01%   gc.g1_old_generation.time(ms)  0
nonheap                    66M      69M      -1       96.56%
codeheap_'non-nmethods'    1M       2M       5M       22.39%
metaspace                  46M      47M      -1       98.01%
Runtime
os.name                                                        Mac OS X
os.version                                                     10.15.4
java.version                                                   15
java.home                                                      /Library/Java/JavaVirtualMachines/jdk-15.jdk/Contents/Home
systemload.average                                             10.68
processors                                                     8
uptime                                                         272s
```

### 数据说明

* ID: Java级别的线程ID，注意这个ID不能跟jstack中的nativeID一一对应。
* NAME: 线程名
* GROUP: 线程组名
* PRIORITY: 线程优先级, 1~10之间的数字，越大表示优先级越高
* STATE: 线程的状态
* CPU%: 线程的cpu使用率。比如采样间隔1000ms，某个线程的增量cpu时间为100ms，则cpu使用率=100/1000=10%
* DELTA_TIME: 上次采样之后线程运行增量CPU时间，数据格式为`秒`
* TIME: 线程运行总CPU时间，数据格式为`分:秒`
* INTERRUPTED: 线程当前的中断位状态
* DAEMON: 是否是daemon线程

#### JVM内部线程
Java 8之后支持获取JVM内部线程CPU时间，这些线程只有名称和CPU时间，没有ID及状态等信息（显示ID为-1）。
通过内部线程可以观测到JVM活动，如GC、JIT编译等占用CPU情况，方便了解JVM整体运行状况。

* 当JVM 堆(heap)/元数据(metaspace)空间不足或OOM时，可以看到GC线程的CPU占用率明显高于其他的线程。
* 当执行`trace/watch/tt/redefine`等命令后，可以看到JIT线程活动变得更频繁。因为JVM热更新class字节码时清除了此class相关的JIT编译结果，需要重新编译。

JVM内部线程包括下面几种：
* JIT编译线程: 如 `C1 CompilerThread0`, `C2 CompilerThread0` 
* GC线程: 如`GC Thread0`, `G1 Young RemSet Sampling`  
* 其它内部线程: 如`VM Periodic Task Thread`, `VM Thread`, `Service Thread`


### 截图展示

![](_static/dashboard.png "dashboard")