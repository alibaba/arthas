thread
======

Check the basic profile and stack trace of the threads.

### Parameters

|Name|Specification|
|---:|:---|
|*id*|thread id in JVM|
|[b]|locate the threads blocking others|
|[n:]|the top n busiest with stack traces|
|[i:]|specify the interval to collect data to compute CPU ratios (ms)|

How to get the CPU ratios?

> Within an *specified* interval, the time cost by the thread compared to the total CPU time. 
> Take a sample (using `java.lang.management.ThreadMXBean#getThreadCpuTime`) to get the CPU time cost for all the threads and after a *specified* interval (default *100 ms*, which can be specified by `-i`), take another sample and we have the CPU time cost and the ratios naturally. 

> Attention: this kind of operation will take time, to decrease the extra cost, you'd better increase the interval to like `5000 ms` for less overhead. 

F.Y.I

If you'd like to check the CPU ratios from the very start of the Java process, [show-busy-java-threads](https://github.com/oldratlee/useful-scripts/blob/master/docs/java.md#-show-busy-java-threads) can be a help. 

### Usage

#### List the top n busiest with detailed stack trace

```bash
$ thread -n 3
"as-command-execute-daemon" Id=28 cpuUsage=79% RUNNABLE
    at sun.management.ThreadImpl.dumpThreads0(Native Method)
    at sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:440)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.processTopBusyThreads(ThreadCommand.java:133)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.process(ThreadCommand.java:79)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.process(AnnotatedCommandImpl.java:82)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.access$100(AnnotatedCommandImpl.java:18)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:111)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:108)
    at com.taobao.arthas.core.shell.system.impl.ProcessImpl$CommandProcessTask.run(ProcessImpl.java:370)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
    at java.lang.Thread.run(Thread.java:745)

    Number of locked synchronizers = 1
    - java.util.concurrent.ThreadPoolExecutor$Worker@1bbc0399


"nioEventLoopGroup-2-2" Id=24 cpuUsage=20% RUNNABLE (in native)
    at sun.nio.ch.KQueueArrayWrapper.kevent0(Native Method)
    at sun.nio.ch.KQueueArrayWrapper.poll(KQueueArrayWrapper.java:198)
    at sun.nio.ch.KQueueSelectorImpl.doSelect(KQueueSelectorImpl.java:103)
    at sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:86)
    -  locked io.netty.channel.nio.SelectedSelectionKeySet@45912c37
    -  locked java.util.Collections$UnmodifiableSet@2684e9b5
    -  locked sun.nio.ch.KQueueSelectorImpl@33292917
    at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:97)
    at io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:62)
    at io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:737)
    at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:392)
    at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884)
    at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
    at java.lang.Thread.run(Thread.java:745)


"Reference Handler" Id=2 cpuUsage=0% WAITING on java.lang.ref.Reference$Lock@3258a272
    at java.lang.Object.wait(Native Method)
    -  waiting on java.lang.ref.Reference$Lock@3258a272
    at java.lang.Object.wait(Object.java:502)
    at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:157)


Affect(row-cnt:0) cost in 128 ms.
```

#### List all info when no options provided

```shell
$ thread
Threads Total: 19, NEW: 0, RUNNABLE: 8, BLOCKED: 0, WAITING: 4, TIMED_WAITING: 7, TERMINATED: 0                                                                                      
ID             NAME                                         GROUP                          PRIORITY       STATE          %CPU           TIME           INTERRUPTED    DAEMON         
28             as-command-execute-daemon                    system                         10             RUNNABLE       89             0:0            false          true           
21             nioEventLoopGroup-3-1                        system                         10             RUNNABLE       10             0:0            false          false          
17             AsyncAppender-Worker-arthas-cache.result.Asy system                         9              WAITING        0              0:0            false          true           
15             Attach Listener                              system                         9              RUNNABLE       0              0:0            false          true           
14             DestroyJavaVM                                main                           5              RUNNABLE       0              0:0            false          false          
3              Finalizer                                    system                         8              WAITING        0              0:0            false          true           
2              Reference Handler                            system                         10             WAITING        0              0:0            false          true           
4              Signal Dispatcher                            system                         9              RUNNABLE       0              0:0            false          true           
9              Thread-0                                     main                           5              TIMED_WAITING  0              0:0            false          false          
10             Thread-1                                     main                           5              TIMED_WAITING  0              0:0            false          false          
11             Thread-2                                     main                           5              TIMED_WAITING  0              0:0            false          false          
12             Thread-3                                     main                           5              TIMED_WAITING  0              0:0            false          false          
13             Thread-4                                     main                           5              TIMED_WAITING  0              0:0            false          false          
19             job-timeout                                  system                         9              TIMED_WAITING  0              0:0            false          true           
20             nioEventLoopGroup-2-1                        system                         10             RUNNABLE       0              0:0            false          false          
24             nioEventLoopGroup-2-2                        system                         10             RUNNABLE       0              0:0            false          false          
27             nioEventLoopGroup-2-3                        system                         10             RUNNABLE       0              0:0            false          false          
22             pool-1-thread-1                              system                         5              TIMED_WAITING  0              0:0            false          false          
23             pool-2-thread-1                              system                         5              WAITING        0              0:0            false          false          
Affect(row-cnt:0) cost in 120 ms.
```

#### thread <thread_id> present the specified thread profile

```bash
$ thread 28
"as-command-execute-daemon" Id=28 RUNNABLE
    at sun.management.ThreadImpl.dumpThreads0(Native Method)
    at sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:440)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.processThread(ThreadCommand.java:146)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.process(ThreadCommand.java:77)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.process(AnnotatedCommandImpl.java:82)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.access$100(AnnotatedCommandImpl.java:18)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:111)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:108)
    at com.taobao.arthas.core.shell.system.impl.ProcessImpl$CommandProcessTask.run(ProcessImpl.java:370)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
    at java.lang.Thread.run(Thread.java:745)

    Number of locked synchronizers = 1
    - java.util.concurrent.ThreadPoolExecutor$Worker@1bbc0399

Affect(row-cnt:0) cost in 14 ms.
```

#### thread -b locate the blocking threads

Using `-b`, we can effectively locate the threads holding locks blocking other threads resulting in a frozen system. 

```bash
$ thread -b
No most blocking thread found!
Affect(row-cnt:0) cost in 12 ms.

## another demo with blocking threads found
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

> Attention: only `synchronized` blocked threads can be located for now, `JUL` not supported yet.


#### thread -i specify the collecting interval

```bash
$ thread -n 3 -i 1000
"as-command-execute-daemon" Id=29 cpuUsage=50% RUNNABLE
    at sun.management.ThreadImpl.dumpThreads0(Native Method)
    at sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:440)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.processTopBusyThreads(ThreadCommand.java:133)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand.process(ThreadCommand.java:79)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.process(AnnotatedCommandImpl.java:82)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.access$100(AnnotatedCommandImpl.java:18)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:111)
    at com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl$ProcessHandler.handle(AnnotatedCommandImpl.java:108)
    at com.taobao.arthas.core.shell.system.impl.ProcessImpl$CommandProcessTask.run(ProcessImpl.java:370)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
    at java.lang.Thread.run(Thread.java:745)

    Number of locked synchronizers = 1
    - java.util.concurrent.ThreadPoolExecutor$Worker@24d17acd


"nioEventLoopGroup-2-2" Id=24 cpuUsage=18% RUNNABLE (in native)
    at sun.nio.ch.KQueueArrayWrapper.kevent0(Native Method)
    at sun.nio.ch.KQueueArrayWrapper.poll(KQueueArrayWrapper.java:198)
    at sun.nio.ch.KQueueSelectorImpl.doSelect(KQueueSelectorImpl.java:103)
    at sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:86)
    -  locked io.netty.channel.nio.SelectedSelectionKeySet@45912c37
    -  locked java.util.Collections$UnmodifiableSet@2684e9b5
    -  locked sun.nio.ch.KQueueSelectorImpl@33292917
    at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:97)
    at io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:62)
    at io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:737)
    at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:392)
    at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884)
    at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
    at java.lang.Thread.run(Thread.java:745)


"nioEventLoopGroup-3-1" Id=21 cpuUsage=14% RUNNABLE (in native)
    at sun.nio.ch.KQueueArrayWrapper.kevent0(Native Method)
    at sun.nio.ch.KQueueArrayWrapper.poll(KQueueArrayWrapper.java:198)
    at sun.nio.ch.KQueueSelectorImpl.doSelect(KQueueSelectorImpl.java:103)
    at sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:86)
    -  locked io.netty.channel.nio.SelectedSelectionKeySet@5642de30
    -  locked java.util.Collections$UnmodifiableSet@18d1b085
    -  locked sun.nio.ch.KQueueSelectorImpl@7c973d15
    at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:97)
    at io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:62)
    at io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:737)
    at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:392)
    at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884)
    at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
    at java.lang.Thread.run(Thread.java:745)


Affect(row-cnt:0) cost in 1013 ms.
```
