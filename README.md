## Arthas

![arthas](site/src/site/sphinx/arthas.png)

[![Build Status](https://travis-ci.org/alibaba/arthas.svg?branch=master)](https://travis-ci.org/alibaba/arthas)
[![codecov](https://codecov.io/gh/alibaba/arthas/branch/master/graph/badge.svg)](https://codecov.io/gh/alibaba/arthas)
[![maven](https://img.shields.io/maven-central/v/com.taobao.arthas/arthas-packaging.svg)](https://search.maven.org/search?q=g:com.taobao.arthas)
![license](https://img.shields.io/github/license/alibaba/arthas.svg)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/alibaba/arthas.svg)](http://isitmaintained.com/project/alibaba/arthas "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/alibaba/arthas.svg)](http://isitmaintained.com/project/alibaba/arthas "Percentage of issues still open")

`Arthas` is a Java Diagnostic tool open sourced by Alibaba.

Arthas allows developers to troubleshoot production issues for Java applications without modifying code or restarting servers.

[中文说明/Chinese Documentation](README_CN.md)

### Background

Often times, the production system network is inaccessible from the local development environment. If issues are encountered in production systems, it is impossible to use IDEs to debug the application remotely. More importantly, debugging in production environment is unacceptable, as it will suspend all the threads, resulting in the suspension of business services. 

Developers could always try to reproduce the same issue on the test/staging environment. However, this is tricky as some issues cannot be reproduced easily on a different environment, or even disappear once restarted. 

And if you're thinking of adding some logs to your code to help troubleshoot the issue, you will have to go through the following lifecycle; test, staging, and then to production. Time is money! This approach is inefficient! Besides, the issue may not be reproducible once the JVM is restarted, as described above.

Arthas was built to solve these issues. A developer can troubleshoot your production issues on-the-fly. No JVM restart, no additional code changes. Arthas works as an observer, which will never suspend your existing threads.

### Key features

* Check whether a class is loaded, or where the class is being loaded. (Useful for troubleshooting jar file conflicts)
* Decompile a class to ensure the code is running as expected.
* View classloader statistics, e.g. the number of classloaders, the number of classes loaded per classloader, the classloader hierarchy, possible classloader leaks, etc.
* View the method invocation details, e.g. method parameter, return object, thrown exception, and etc.
* Check the stack trace of specified method invocation. This is useful when a developers wants to know the caller of the said method.
* Trace the method invocation to find slow sub-invocations.
* Monitor method invocation statistics, e.g. qps, rt, success rate and etc.
* Monitor system metrics, thread states and cpu usage, gc statistics, and etc.
* Supports command line interactive mode, with auto-complete feature enabled.
* Supports telnet and websocket, which enables both local and remote diagnostics with command line and browsers.
* Supports profiler/Flame Graph
* Supports JDK 6+.
* Supports Linux/Mac/Windows.


### Online Tutorials(Recommend)

* [Arthas Basics](https://alibaba.github.io/arthas/arthas-tutorials?language=en&id=arthas-basics)
* [Arthas Advanced](https://alibaba.github.io/arthas/arthas-tutorials?language=en&id=arthas-advanced)

### Quick start

#### Use `arthas-boot`(Recommend)

Download`arthas-boot.jar`，Start with `java` command:

```bash
curl -O https://alibaba.github.io/arthas/arthas-boot.jar
java -jar arthas-boot.jar
```

Print usage:

```bash
java -jar arthas-boot.jar -h
```

#### Use `as.sh`

You can install Arthas with one single line command on Linux, Unix, and Mac. Copy the following command and paste it into the command line, then press *Enter* to run:

```bash
curl -L https://alibaba.github.io/arthas/install.sh | sh
```

The command above will download the bootstrap script `as.sh` to the current directory. You can move it the any other place you want, or put its location in `$PATH`.

You can enter its interactive interface by executing `as.sh`, or execute `as.sh -h` for more help information.


### Documentation

* [Online Tutorials(Recommend)](https://alibaba.github.io/arthas/arthas-tutorials?language=en)
* [User manual](https://alibaba.github.io/arthas/en)
* [Installation](https://alibaba.github.io/arthas/en/install-detail.html)
* [Download](https://alibaba.github.io/arthas/en/download.html)
* [Quick start](https://alibaba.github.io/arthas/en/quick-start.html)
* [Advanced usage](https://alibaba.github.io/arthas/en/advanced-use.html)
* [Commands](https://alibaba.github.io/arthas/en/commands.html)
* [WebConsole](https://alibaba.github.io/arthas/en/web-console.html)
* [Docker](https://alibaba.github.io/arthas/en/docker.html)
* [User cases](https://github.com/alibaba/arthas/issues?q=label%3Auser-case)
* [Questions and answers](https://github.com/alibaba/arthas/issues?utf8=%E2%9C%93&q=label%3Aquestion-answered+)
* [Compile and debug/How to contribute](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)
* [Release Notes](https://github.com/alibaba/arthas/releases)


### Feature Showcase

#### Dashboard

* https://alibaba.github.io/arthas/en/dashboard

![dashboard](site/src/site/sphinx/_static/dashboard.png)

#### Thread

* https://alibaba.github.io/arthas/en/thread

See what is eating your cpu (ranked by top cpu usage) and what is going on there in one glance:

```bash
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

* https://alibaba.github.io/arthas/en/jad

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

#### mc
* https://alibaba.github.io/arthas/en/mc

Memory compiler, compiles `.java` files into `.class` files in memory.

```bash
mc /tmp/Test.java
```

#### redefine

* https://alibaba.github.io/arthas/en/redefine

Load the external `*.class` files to re-define the loaded classes in JVM.

```bash
redefine /tmp/Test.class
redefine -c 327a647b /tmp/Test.class /tmp/Test\$Inner.class
```

#### sc

* https://alibaba.github.io/arthas/en/sc

Search any loaded class with detailed information.

```bash
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

* https://alibaba.github.io/arthas/en/stack

View the call stack of `test.arthas.TestStack#doGet`:

```bash
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

* https://alibaba.github.io/arthas/en/trace

See what is slowing down your method invocation with trace command:

![trace](site/src/site/sphinx/_static/trace.png)

#### Watch

* https://alibaba.github.io/arthas/en/watch

Watch the first parameter and thrown exception of `test.arthas.TestWatch#doGet` only if it throws exception.

```bash
$ watch test.arthas.TestWatch doGet {params[0], throwExp} -e
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 65 ms.
ts=2018-09-18 10:26:28;result=@ArrayList[
    @RequestFacade[org.apache.catalina.connector.RequestFacade@79f922b2],
    @NullPointerException[java.lang.NullPointerException],
]
```

#### Monitor

* https://alibaba.github.io/arthas/en/monitor

Monitor a specific method invocation statistics, including total number of invocations, average response time, success rate, and every 5 seconds:

```bash
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

* https://alibaba.github.io/arthas/en/tt

Record method invocation data, so that you can check the method invocation parameters, returned value, and thrown exceptions later. It works as if you could come back and replay the past method invocation via time tunnel.

```bash
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

* https://alibaba.github.io/arthas/en/classloader

```bash
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

* https://alibaba.github.io/arthas/en/web-console

![web console](site/src/site/sphinx/_static/web-console-local.png)


#### Profiler/FlameGraph

* https://alibaba.github.io/arthas/en/profiler

```bash
$ profiler start
Started [cpu] profiling
```

```
$ profiler stop
profiler output file: /tmp/demo/arthas-output/20191125-135546.svg
OK
```

View profiler results under arthas-output via browser:

![](site/src/site/sphinx/_static/arthas-output-svg.jpg)



### Known Users

Welcome to register the company name in this issue: https://github.com/alibaba/arthas/issues/111 (in order of registration)

![Alibaba](static/alibaba.png)
![Alipay](static/alipay.png)
![Aliyun](static/aliyun.png)
![Taobao](static/taobao.png)
![Tmall](static/tmall.png)
![微医](static/weiyi.png)
![卓越教育](static/zhuoyuejiaoyu.png)
![狐狸金服](static/hulijingfu.png)
![三体云](static/santiyun.png)
![证大文化](static/zhengdawenhua.png)
![连连支付](static/lianlianpay.png)
![Acmedcare+](static/acmedcare.png)
![好慷](static/homeking365_log.png)
![来电科技](static/laidian.png)
![四格互联](static/sigehulian.png)
![ICBC](static/icbc.png)
![陆鹰](static/luying.png)
![玩友时代](static/wangyoushidai.png)
![她社区](static/tashequ.png)
![龙腾出行](static/longtengchuxing.png)
![foscam](static/foscam.png)
![二维火](static/2dfire.png)
![lanxum](static/lanxum_com.png)
![纳里健康](static/ngarihealth.png)
![掌门1对1](static/zhangmen.png)
![offcn](static/offcn.png)
![sia](static/sia.png)
![振安资产](static/zhenganzichang.png)
![菠萝](static/bolo.png)
![中通快递](static/zto.png)
![光点科技](static/guangdian.png)
![广州工程技术职业学院](static/gzvtc.jpg)
![mstar](static/mstar.png)
![xwbank](static/xwbank.png)
![imexue](static/imexue.png)
![keking](static/keking.png)
![secoo](static/secoo.jpg)
![viax](static/viax.png)
![yanedu](static/yanedu.png)
![duia](static/duia.png)
![哈啰出行](static/hellobike.png)
![hollycrm](static/hollycrm.png)
![citycloud](static/citycloud.jpg)
![yidianzixun](static/yidianzixun.png)
![神州租车](static/zuche.png)
![天眼查](static/tianyancha.png)
![商脉云](static/anjianyun.png)
![三新文化](static/sanxinbook.png)
![雪球财经](static/xueqiu.png)
![百安居](static/bthome.png)
![安心保险](static/95303.png)
![杭州源诚科技](static/hzyc.png)
![91moxie](static/91moxie.png)
![智慧开源](static/wisdom.png)
![富佳科技](static/fujias.png)
![鼎尖软件](static/dingjiansoft.png)
![广通软件](static/broada.png)
![九鼎瑞信](static/evercreative.jpg)
![小米有品](static/xiaomiyoupin.png)
![欧冶云商](static/ouyeel.png)
![投投科技](static/toutou.png)
![饿了么](static/ele.png)
![58同城](static/58.png)
![上海浪沙](static/runsa.png)
![符律科技](static/fhldtech.png)
![顺丰科技](static/sf.png)
![新致软件](static/newtouch.png)
![北京华宇信息](static/thunisoft.png)
![太平洋保险](static/cpic.png)
![旅享网络](static/risingch.png)
![水滴互联](static/shuidihuzhu.png)
![贝壳找房](static/ke.png)
![嘟嘟牛](static/dodonew.png)
![云幂信息](static/yunmixinxi.png)
![随手科技](static/sui.png)
![妈妈去哪儿](static/mamaqunaer.jpg)
![云实信息](static/realscloud.png)
![BBD数联铭品](static/bbdservice.png)
![伙伴集团](static/zhaoshang800.png)
![数梦工场](static/dtdream.png)
![安恒信息](static/dbappsecurity.png)
![亚信科技](static/asiainfo.png)
![云舒写](static/yunshuxie.png)
![微住](static/iweizhu.png)
![月亮小屋](static/bluemoon.png)
![大搜车](static/souche.png)
![今日图书](static/jinritushu.png)
![竹间智能](static/emotibot.png)
![数字认证](static/bjca.png)
![360金融](static/360jinrong.png)
![安居客](static/anjuke.jpg)
![qunar](static/qunar.png)
![ctrip](static/ctrip.png)
![Tuniu](static/tuniu.png)
![多点](static/dmall.jpg)
![转转](static/zhuanzhuan.jpg)
![金蝶](static/kingdee.jpg)
![华清飞扬](static/sincetimes.jpg)
![神奇视角](static/fasterar.jpg)
![南京昂克软件](static/angke.jpg)
![网盛生意宝](static/netsun.jpg)
![北京登云美业网络](static/idengyun.jpg)
![Holder](static/holder.png)
![立林科技](static/leelen.png)
![爱成长](static/aichengzhang.png)
![嘉云数据](static/clubfactory.png)
![百草味](static/bcw.png)
![青岛优米](static/youmi.png)
![紫光软件](static/unis.png)
![拓保软件](static/tobosoft.png)
![海信集团](static/hisense.png)
![小红唇](static/xiaohongchun.png)
![上海恺英](static/kaiying.png)
![上海慧力](static/xiaohuasheng.png)
![上海喔噻](static/shouqingba.png)
![vipkid](static/vipkid.png)
![宇中科技](static/yuzhong.png)

### Derivative Projects

* [Bistoury: A project that integrates Arthas](https://github.com/qunarcorp/bistoury)
* [A fork of arthas using MVEL](https://github.com/XhinLiang/arthas)

### Credit

#### Contributors

This project exists thanks to all the people who contribute.

<a href="https://github.com/alibaba/arthas/graphs/contributors"><img src="https://opencollective.com/arthas/contributors.svg?width=890&button=false" /></a>

#### Projects

* [greys-anatomy](https://github.com/oldmanpushcart/greys-anatomy): The Arthas code base has derived from Greys, we thank for the excellent work done by Greys.
* [termd](https://github.com/alibaba/termd): Arthas's terminal implementation is based on termd, an open source library for writing terminal applications in Java.
* [crash](https://github.com/crashub/crash): Arthas's text based user interface rendering is based on codes extracted from [here](https://github.com/crashub/crash/tree/1.3.2/shell)
* [cli](https://github.com/alibaba/cli): Arthas's command line interface implementation is based on cli, open sourced by vert.x
* [compiler](https://github.com/skalogs/SkaETL/tree/master/compiler) Arthas's memory compiler.
* [Apache Commons Net](https://commons.apache.org/proper/commons-net/) Arthas's telnet client.
* [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) Arthas's profiler command.
