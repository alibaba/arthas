## Arthas

![arthas](site/src/site/sphinx/arthas.png)

`Arthas` is an Java Diagnostic tool open sourced by Alibaba.

Arthas can help developer trouble-shooting production issues for Java applications without modifying your code or restating your server.

[中文说明](README_CN.md)

### Background

In production system, sometimes the networks is unreachable from local development environment. If you encounter some issues in production system, it is impossible to use IDE to debug the application remotely. More importantly, debugging in production is unacceptable, because it will suspend all the threads, which leads to business unavailability. 

Some may think of reproducing the same issue on the test/staging environment, however, some tricky issue either can hardly be reproduced on a different environment, or even disappeared once restarted. 

Thinking of adding some logs to your code? You have to go through test, staging, and then on to production. Time is money! That is a lot inefficient! Besides, the issue may not be reproducible once restart your JVM, as described above.

Arthas is born to solve these issues. You can trouble-shoot your production issue on-the-fly. No JVM restart, no additional code changes. Arthas works as an observer, which will never suspend your existing threads.

### Key features

* Check whether a class is loaded? And whether is class loaded from? (Useful for trouble-shooting jar file conflicts)
* Decompile a class to ensure the code is running as expected.
* View classloader statistics, e.g. how may classloaders are there? how many class is loaded per classloader? What is the classloader hierarchy? Is there possible classloader leaks?
* View the method invocation details, e.g. method parameter, return object, thrown exception, and etc.
* Check the stack trace of specified method invocation. This is useful when you would like to know who is calling your method?
* Trace the method invocation to find slow sub-invocations.
* Monitor method invcation statistics, e.g. qps, rt, success rate and etc.
* Monitoring your system metrics, thread states and cpu usage, gc statistics, and etc.
* Support command line interactive mode, with auto completed feature enabled.
* Support telnet and websocket, which enables both local and remote diagnostics with command line and browsers.

### Quick start

#### Linux/Unix/Mac

Install Arthas:

```
curl -L https://alibaba.github.io/arthas/install.sh | sh
```

Start Arthas:

```
./as.sh
```

#### Windows

1. Click [here](http://search.maven.org/classic/#search%7Cga%7C1%7Cg%3A%22com.taobao.arthas%22%20AND%20a%3A%22arthas-packaging%22) to Download the latest binary releases.
2. Unzip the file.
3. Go to the bin directory
4. run the following command `as.bat $PID`


### Documentation

English version is on the way, if you would like to contribute, please leave a message [here](https://github.com/alibaba/arthas/issues/51)

* [User manual](https://alibaba.github.io/arthas/en)
* [Installation](https://alibaba.github.io/arthas/en/install-detail.html)
* [Quick start](https://alibaba.github.io/arthas/en/quick-start.html)
* [Advanced usage](https://alibaba.github.io/arthas/en/advanced-use.html)
* [Questions and answers](https://github.com/alibaba/arthas/issues?utf8=%E2%9C%93&q=label%3Aquestion-answered+)
* [How to contribute](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)


### Feature Showcase

#### Dashboard

* https://alibaba.github.io/arthas/dashboard

![dashboard](site/src/site/sphinx/_static/dashboard.png)

#### Thread

See who is eating your cpu (ranked by top cpu usage) and what is going on there in one glance:

```
$ thread -n 3
"as-command-execute-daemon" Id=29 cpuUsage=75% RUNNABLE
    at sun.management.ThreadImpl.dumpThreads0(Native Method)
    at sun.management.ThreadImpl.getThreadInfo(ThreadImpl.java:440)
    at com.taobao.arthas.core.command.monitor200.ThreadCommand$1.action(ThreadCommand.java:58)
    at com.taobao.arthas.core.command.handler.AbstractCommandHandler.execute(AbstractCommandHandler.java:238)
    at com.taobao.arthas.core.command.handler.DefaultCommandHandler.handleCommand(DefaultCommandHandler.java:67)
    at com.taobao.arthas.core.server.ArthasServer$4.run(ArthasServer.java:276)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
    at java.lang.Thread.run(Thread.java:745)

    Number of locked synchronizers = 1
    - java.util.concurrent.ThreadPoolExecutor$Worker@6cd0b6f8

"as-session-expire-daemon" Id=25 cpuUsage=24% TIMED_WAITING
    at java.lang.Thread.sleep(Native Method)
    at com.taobao.arthas.core.server.DefaultSessionManager$2.run(DefaultSessionManager.java:85)

"Reference Handler" Id=2 cpuUsage=0% WAITING on java.lang.ref.Reference$Lock@69ba0f27
    at java.lang.Object.wait(Native Method)
    -  waiting on java.lang.ref.Reference$Lock@69ba0f27
    at java.lang.Object.wait(Object.java:503)
    at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:133)
```

#### jad

Decompile your class with one shot:

```java
$ jad javax.servlet.Servlet

ClassLoader:
+-java.net.URLClassLoader@6108b2d7
  +-sun.misc.Launcher$AppClassLoader@18b4aac2
    +-sun.misc.Launcher$ExtClassLoader@1ddf84b8

Location:
/Users/xxx/work/test/lib/servlet-api.jar

/*
 * Decompiled with CFR 0_122.
 */
package javax.servlet;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface Servlet {
    public void init(ServletConfig var1) throws ServletException;

    public ServletConfig getServletConfig();

    public void service(ServletRequest var1, ServletResponse var2) throws ServletException, IOException;

    public String getServletInfo();

    public void destroy();
}
```

#### sc

Search any loaded class with detailed infomation.

```
$ sc -d org.springframework.web.context.support.XmlWebApplicationContext
 class-info        org.springframework.web.context.support.XmlWebApplicationContext
 code-source       /Users/xxx/work/test/WEB-INF/lib/spring-web-3.2.11.RELEASE.jar
 name              org.springframework.web.context.support.XmlWebApplicationContext
 isInterface       false
 isAnnotation      false
 isEnum            false
 isAnonymousClass  false
 isArray           false
 isLocalClass      false
 isMemberClass     false
 isPrimitive       false
 isSynthetic       false
 simple-name       XmlWebApplicationContext
 modifier          public
 annotation
 interfaces
 super-class       +-org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
                     +-org.springframework.context.support.AbstractRefreshableConfigApplicationContext
                       +-org.springframework.context.support.AbstractRefreshableApplicationContext
                         +-org.springframework.context.support.AbstractApplicationContext
                           +-org.springframework.core.io.DefaultResourceLoader
                             +-java.lang.Object
 class-loader      +-org.apache.catalina.loader.ParallelWebappClassLoader
                     +-java.net.URLClassLoader@6108b2d7
                       +-sun.misc.Launcher$AppClassLoader@18b4aac2
                         +-sun.misc.Launcher$ExtClassLoader@1ddf84b8
 classLoaderHash   25131501

```

#### stack

View the call stack of `test.arthas.TestStack#doGet`:

```
$ stack test.arthas.TestStack doGet
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 286 ms.
ts=2018-09-18 10:11:45;thread_name=http-bio-8080-exec-10;id=d9;is_daemon=true;priority=5;TCCL=org.apache.catalina.loader.ParallelWebappClassLoader@25131501
    @test.arthas.TestStack.doGet()
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:624)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:731)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:303)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:208)
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:241)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:208)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:241)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:208)
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:220)
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:110)
        ...
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:169)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:103)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:116)
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:451)
        at org.apache.coyote.http11.AbstractHttp11Processor.process(AbstractHttp11Processor.java:1121)
        at org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process(AbstractProtocol.java:637)
        at org.apache.tomcat.util.net.JIoEndpoint$SocketProcessor.run(JIoEndpoint.java:316)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
        at java.lang.Thread.run(Thread.java:745)
```

#### Trace

See who is slowing down your method invocation with trace command:

![trace](site/src/site/sphinx/_static/trace.png)

#### Watch

Watch the first parameter and thrown exception of `test.arthas.TestWatch#doGet` only if it throws exception.

```
$ watch test.arthas.TestWatch doGet {params[0], throwExp} -e
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 65 ms.
ts=2018-09-18 10:26:28;result=@ArrayList[
    @RequestFacade[org.apache.catalina.connector.RequestFacade@79f922b2],
    @NullPointerException[java.lang.NullPointerException],
]
```

#### Monitor

Monitor a specific method invoction statistics, including total number of invocations, avarage response time, success rate, every 5 seconds:

```
$ monitor -c 5 org.apache.dubbo.demo.provider.DemoServiceImpl sayHello
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 109 ms.
 timestamp            class                                           method    total  success  fail  avg-rt(ms)  fail-rate
----------------------------------------------------------------------------------------------------------------------------
 2018-09-20 09:45:32  org.apache.dubbo.demo.provider.DemoServiceImpl  sayHello  5      5        0     0.67        0.00%

 timestamp            class                                           method    total  success  fail  avg-rt(ms)  fail-rate
----------------------------------------------------------------------------------------------------------------------------
 2018-09-20 09:45:37  org.apache.dubbo.demo.provider.DemoServiceImpl  sayHello  5      5        0     1.00        0.00%

 timestamp            class                                           method    total  success  fail  avg-rt(ms)  fail-rate
----------------------------------------------------------------------------------------------------------------------------
 2018-09-20 09:45:42  org.apache.dubbo.demo.provider.DemoServiceImpl  sayHello  5      5        0     0.43        0.00%
```

#### Time Tunnel(tt)

Recording method invocation data, so that you can check the method invacation parameters, returned value and thrown exception later. It works as if you could come back and replay the past method invacation via time tunnel.

```
$ tt -t org.apache.dubbo.demo.provider.DemoServiceImpl sayHello
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 75 ms.
 INDEX   TIMESTAMP            COST(ms)  IS-RET  IS-EXP   OBJECT         CLASS                          METHOD
-------------------------------------------------------------------------------------------------------------------------------------
 1000    2018-09-20 09:54:10  1.971195  true    false    0x55965cca     DemoServiceImpl                sayHello
 1001    2018-09-20 09:54:11  0.215685  true    false    0x55965cca     DemoServiceImpl                sayHello
 1002    2018-09-20 09:54:12  0.236303  true    false    0x55965cca     DemoServiceImpl                sayHello
 1003    2018-09-20 09:54:13  0.159598  true    false    0x55965cca     DemoServiceImpl                sayHello
 1004    2018-09-20 09:54:14  0.201982  true    false    0x55965cca     DemoServiceImpl                sayHello
 1005    2018-09-20 09:54:15  0.214205  true    false    0x55965cca     DemoServiceImpl                sayHello
 1006    2018-09-20 09:54:16  0.241863  true    false    0x55965cca     DemoServiceImpl                sayHello
 1007    2018-09-20 09:54:17  0.305747  true    false    0x55965cca     DemoServiceImpl                sayHello
 1008    2018-09-20 09:54:18  0.18468   true    false    0x55965cca     DemoServiceImpl                sayHello
```

#### Classloader

```
$ classloader
 name                                                  numberOfInstances  loadedCountTotal
 BootstrapClassLoader                                  1                  3346
 com.taobao.arthas.agent.ArthasClassloader             1                  1262
 java.net.URLClassLoader                               2                  1033
 org.apache.catalina.loader.ParallelWebappClassLoader  1                  628
 sun.reflect.DelegatingClassLoader                     166                166
 sun.misc.Launcher$AppClassLoader                      1                  31
 com.alibaba.fastjson.util.ASMClassLoader              6                  15
 sun.misc.Launcher$ExtClassLoader                      1                  7
 org.jvnet.hk2.internal.DelegatingClassLoader          2                  2
 sun.reflect.misc.MethodUtil                           1                  1
```

#### Web Console

* https://alibaba.github.io/arthas/web-console

![web console](site/src/site/sphinx/_static/web-console-local.png)


### Credit

* [greys-anatomy](https://github.com/oldmanpushcart/greys-anatomy): The Arthas code base has derived from Greys, we thank for the excellent work done by Greys.
* [termd](https://github.com/termd/termd): Arthas's termnial implementation is based on termd, an open source library for writing terminal applications in Java.
* [crash](https://github.com/crashub/crash): Arthas's text based user interface rendering is based on codes extracted from [here](https://github.com/crashub/crash/tree/1.3.2/shell)
* [cli](https://github.com/eclipse-vertx/vert.x/tree/master/src/main/java/io/vertx/core/cli): Arthas's command line interface implementation is based on cli, open sourced by vert.x