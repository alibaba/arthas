
### 支持一键展示当前最忙的前N个线程并打印堆栈：

`thread -n 3`{{execute T2}}

```bash
$ thread -n 3
"C1 CompilerThread0" [Internal] cpuUsage=1.63% deltaTime=3ms time=1170ms


"arthas-command-execute" Id=23 cpuUsage=0.11% deltaTime=0ms time=401ms RUNNABLE
    at java.management@11.0.7/sun.management.ThreadImpl.dumpThreads0(Native Method)
    at java.management@11.0.7/sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:466)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.processTopBusyThreads(ThreadCommand.java:199)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.process(ThreadCommand.java:122)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.process(AnnotatedCommandImpl.java:82)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.access$100(AnnotatedCommandImpl.java:18)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:111)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:108)
    at com.taobao.arthas.core.shell.system.impl.ProcessImpl$CommandProcessTask.run(ProcessImpl.java:385)
    at java.base@11.0.7/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515)
    at java.base@11.0.7/java.util.concurrent.FutureTask.run(FutureTask.java:264)
    at java.base@11.0.7/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304)
    at java.base@11.0.7/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
    at java.base@11.0.7/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
    at java.base@11.0.7/java.lang.Thread.run(Thread.java:834)


"VM Periodic Task Thread" [Internal] cpuUsage=0.07% deltaTime=0ms time=584ms
```

* 没有线程ID，包含`[Internal]`表示为JVM内部线程，参考`dashboard`命令的介绍。
* `cpuUsage`为采样间隔时间内线程的CPU使用率，与`dashboard`命令的数据一致。
* `deltaTime`为采样间隔时间内线程的增量CPU时间，小于1ms时被取整显示为0ms。
* `time` 线程运行总CPU时间。

注意：线程栈为第二采样结束时获取，不能表明采样间隔时间内该线程都是在处理相同的任务。建议间隔时间不要太长，可能间隔时间越大越不准确。
可以根据具体情况尝试指定不同的间隔时间，观察输出结果。

### 当没有参数时，显示第一页线程信息

默认按照CPU增量时间降序排列，只显示第一页数据，避免滚屏。

`thread`{{execute T2}}

```bash
$ thread
Threads Total: 33, NEW: 0, RUNNABLE: 9, BLOCKED: 0, WAITING: 3, TIMED_WAITING: 4, TERMINATED: 0, Internal threads: 17
ID   NAME                           GROUP          PRIORITY  STATE     %CPU      DELTA_TIME TIME      INTERRUPT DAEMON
-1   C2 CompilerThread0             -              -1        -         5.06      0.010      0:0.973   false     true
-1   C1 CompilerThread0             -              -1        -         0.95      0.001      0:0.603   false     true
23   arthas-command-execute         system         5         RUNNABLE  0.17      0.000      0:0.226   false     true
-1   VM Periodic Task Thread        -              -1        -         0.05      0.000      0:0.094   false     true
-1   Sweeper thread                 -              -1        -         0.04      0.000      0:0.011   false     true
-1   G1 Young RemSet Sampling       -              -1        -         0.02      0.000      0:0.025   false     true
12   Attach Listener                system         9         RUNNABLE  0.0       0.000      0:0.022   false     true
11   Common-Cleaner                 InnocuousThrea 8         TIMED_WAI 0.0       0.000      0:0.000   false     true
3    Finalizer                      system         8         WAITING   0.0       0.000      0:0.000   false     true
2    Reference Handler              system         10        RUNNABLE  0.0       0.000      0:0.000   false     true
4    Signal Dispatcher              system         9         RUNNABLE  0.0       0.000      0:0.000   false     true
15   arthas-NettyHttpTelnetBootstra system         5         RUNNABLE  0.0       0.000      0:0.029   false     true
22   arthas-NettyHttpTelnetBootstra system         5         RUNNABLE  0.0       0.000      0:0.196   false     true
24   arthas-NettyHttpTelnetBootstra system         5         RUNNABLE  0.0       0.000      0:0.038   false     true
16   arthas-NettyWebsocketTtyBootst system         5         RUNNABLE  0.0       0.000      0:0.001   false     true
17   arthas-NettyWebsocketTtyBootst system         5         RUNNABLE  0.0       0.000      0:0.001   false     true
```

### thread --all, 显示所有匹配的线程

显示所有匹配线程信息，有时需要获取全部JVM的线程数据进行分析。

`thread --all`{{execute T2}}

### thread id， 显示指定线程的运行堆栈

查看线程ID 16的栈：

`thread 16`{{execute T2}}

```bash
$ thread 1
"main" Id=1 WAITING on java.util.concurrent.CountDownLatch$Sync@29fafb28
    at sun.misc.Unsafe.park(Native Method)
    -  waiting on java.util.concurrent.CountDownLatch$Sync@29fafb28
    at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly(AbstractQueuedSynchronizer.java:997)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1304)
    at java.util.concurrent.CountDownLatch.await(CountDownLatch.java:231)
```

### thread -b, 找出当前阻塞其他线程的线程

有时候我们发现应用卡住了， 通常是由于某个线程拿住了某个锁， 并且其他线程都在等待这把锁造成的。 为了排查这类问题， arthas提供了`thread -b`， 一键找出那个罪魁祸首。

`thread -b`{{execute T2}}

```bash
$ thread -b
"http-bio-8080-exec-4" Id=27 TIMED_WAITING
    at java.lang.Thread.sleep(Native Method)
    at test.arthas.TestThreadBlocking.doGet(TestThreadBlocking.java:22)
    -  locked java.lang.Object@725be470 <---- but blocks 4 other threads!
    at javax.servlet.http.HttpServlet.service(HttpServlet.java:624)
    at javax.servlet.http.HttpServlet.service(HttpServlet.java:731)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:303)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:208)
    at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:241)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:208)
    at test.filter.TestDurexFilter.doFilter(TestDurexFilter.java:46)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:241)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:208)
    at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:220)
    at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:122)
    at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:505)
    at com.taobao.tomcat.valves.ContextLoadFilterValve$FilterChainAdapter.doFilter(ContextLoadFilterValve.java:191)
    at com.taobao.eagleeye.EagleEyeFilter.doFilter(EagleEyeFilter.java:81)
    at com.taobao.tomcat.valves.ContextLoadFilterValve.invoke(ContextLoadFilterValve.java:150)
    at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:170)
    at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:103)
    at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:116)
    at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:429)
    at org.apache.coyote.http11.AbstractHttp11Processor.process(AbstractHttp11Processor.java:1085)
    at org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process(AbstractProtocol.java:625)
    at org.apache.tomcat.util.net.JIoEndpoint$SocketProcessor.run(JIoEndpoint.java:318)
    -  locked org.apache.tomcat.util.net.SocketWrapper@7127ee12
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
    at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
    at java.lang.Thread.run(Thread.java:745)
 
    Number of locked synchronizers = 1
    - java.util.concurrent.ThreadPoolExecutor$Worker@31a6493e
```

**注意**， 目前只支持找出synchronized关键字阻塞住的线程， 如果是`java.util.concurrent.Lock`， 目前还不支持。

### thread -i, 指定采样时间间隔
* `thread -i 1000` : 统计最近1000ms内的线程CPU时间。
 
`thread -i 1000`{{execute T2}}

* `thread -n 3 -i 1000` : 列出1000ms内最忙的3个线程栈
 
`thread -n 3 -i 1000`{{execute T2}}

```bash
$ thread -n 3 -i 1000
"as-command-execute-daemon" Id=4759 cpuUsage=23% RUNNABLE
    at sun.management.ThreadImpl.dumpThreads0(Native Method)
    at sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:440)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.processTopBusyThreads(ThreadCommand.java:133)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.process(ThreadCommand.java:79)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.process(AnnotatedCommandImpl.java:96)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.access$100(AnnotatedCommandImpl.java:27)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:125)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:122)
    at com.taobao.arthas.core.shell.system.impl.ProcessImpl$CommandProcessTask.run(ProcessImpl.java:332)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
    at java.lang.Thread.run(Thread.java:756)
 
    Number of locked synchronizers = 1
    - java.util.concurrent.ThreadPoolExecutor$Worker@546aeec1
...
```

### thread –state ，查看指定状态的线程

`thread --state WAITING`{{execute T2}}

```bash
[arthas@28114]$ thread --state WAITING
Threads Total: 16, NEW: 0, RUNNABLE: 9, BLOCKED: 0, WAITING: 3, TIMED_WAITING: 4, TERMINATED: 0
ID   NAME                           GROUP           PRIORITY   STATE     %CPU      DELTA_TIME TIME      INTERRUPTE DAEMON
3    Finalizer                      system          8          WAITING   0.0       0.000      0:0.000   false      true
20   arthas-UserStat                system          9          WAITING   0.0       0.000      0:0.001   false      true
14   arthas-timer                   system          9          WAITING   0.0       0.000      0:0.000   false      true
```
