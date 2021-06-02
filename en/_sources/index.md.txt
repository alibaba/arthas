Arthas Documentation
===

**[中文文档/Chinese Documentation](https://arthas.aliyun.com/doc/)**

![](arthas.png)

Arthas is a Java diagnostic tool open-sourced by Alibaba middleware team. It is widely adopted and popular among the developers inside Alibaba. Arthas helps developers in trouble-shooting issues in production environment for Java based applications without modifying code or restarting servers.

### Background

Oftentimes the production system network is inaccessible from local development environment. If issues are encountered in production systems, it is impossible to use IDE to debug the application remotely. What's even worse, debugging in production environment is unacceptable, as it will suspend all the threads, leading to services downtime.

Developers could always try to reproduce the same issue on the test/staging environment. However, this is tricky as some issues cannot be reproduced easily in a different environment, or even disappear once restarted. 

And if you're thinking of adding some logs to your code to help trouble-shoot the issue, you will have to go through the following lifecycle: test, staging, and then to production. Time is money! This approach is inefficient! Worse still, the issue may not be fixed since it might be irreproducible once the JVM is restarted, as described above.

Arthas is built to solve these issues. A developer can troubleshoot production issues on the fly. No JVM restart, no additional code changes. Arthas works as an observer, that is, it will never suspend your running threads.

### Key features

* Check whether a class is loaded? Or where the class is loaded from? (Useful for trouble-shooting jar file conflicts)
* Decompile a class to ensure the code is running as expected.
* Check classloader statistics, e.g. the number of classloaders, the number of classes loaded per classloader, the classloader hierarchy, possible classloader leaks, etc.
* Check the method invocation details, e.g. method parameter, returned values, exceptions and etc.
* Check the stack trace of specified method invocation. This is useful when a developer wants to know the caller of the method.
* Trace the method invocation to find slow sub-invocations.
* Monitor method invocation statistics, e.g. QPS (Query Per Second), RT (Return Time), success rate and etc.
* Monitor system metrics, thread states and CPU usage, GC statistics and etc.
* Supports command line interactive mode, with auto-complete feature enabled.
* Supports telnet and WebSocket, which enables both local and remote diagnostics with command line and browsers.
* Supports profiler/Flame Graph
* Support get objects in the heap that are instances of the specified class. 
* Supports JDK 6+
* Supports Linux/Mac/Windows


**If you are using Arthas, please let us know. Your feedback is very important to us: [View](https://github.com/alibaba/arthas/issues/111)**

Contents
--------

* [Home](https://arthas.aliyun.com/)
* [Online tutorials(Recommend)](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en)
* [Installation](install-detail.md)
* [Download](download.md)
* [Quick start](quick-start.md)
* [Advanced usage](advanced-use.md)
* [Commands](commands.md)
* [WebConsole](web-console.md)
* [Arthas Tunnel](tunnel.md)
* [Http API](http-api.md)
* [Docker](docker.md)
* [Arthas Spring Boot Starter](spring-boot-starter.md)
* [IDEA Plugin](idea-plugin.md)
* [User cases](https://github.com/alibaba/arthas/issues?q=label%3Auser-case)
* [FAQ](faq.md)
* [Star me at GitHub](https://github.com/alibaba/arthas)
* [Compile and debug/CONTRIBUTING](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md#)
* [Release Notes](https://github.com/alibaba/arthas/releases)
* [Contact us](contact-us.md)
