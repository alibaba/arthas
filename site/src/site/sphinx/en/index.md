Arthas Documentation
===

**[中文文档](https://alibaba.github.io/arthas/)**

![arthas](arthas.png)

Arthas is a Java diagnostic tool open-sourced by Alibaba middleware team. It is widely adopted and popular among the developers inside Alibaba. Arthas helps developers in trouble-shooting issues in production environment for Java based applications without modifying code or restarting servers.

### Background

Oftentimes the production system network is inaccessible from local development environment. If issues are encountered in production systems, it is impossible to use IDE to debug the application remotely. Moreover, debugging in production environment is unacceptable, as it will suspend all the threads, which leads to block business services. 

Developers could always try to reproduce the same issue on the test/staging environment. However, this is tricky as some issues cannot be reproduced easily in a different environment, or even disappear once restarted. 

And if you're thinking of adding some logs to your code to help troubleshoot the issue, you will have to go through the following lifecycle: test, staging, and then to production. Time is money! This approach is inefficient. Besides, the issue may not be reproducible once the JVM is restarted, as described above.

Arthas is built to solve these issues. A developer can troubleshoot production issues on the fly. No JVM restart, no additional code changes. Arthas works as an observer, that is, it will never suspend your running threads.

### Key features

* Check whether a class is loaded? Or where the class is loaded from? Useful for troubleshooting jar conflicts.
* Decompile a class to ensure the running code is expected.
* View classloader statistics, e.g. the number of classloaders, the number of classes loaded per classloader, the classloader hierarchy, possible classloader leaks, etc.
* View the method invocation details, e.g. method parameter, return object, thrown exception, and etc.
* Check the stack trace of specified method invocation. This is useful when a developers wants to know the caller of the target method.
* Trace the method invocation to find slow path.
* Monitor method invocation statistics, e.g. QPS, RT, success rate, and etc.
* Monitor system metrics, thread states and CPU usage, GC statistics, and etc.
* Support command line interactive mode, with auto-completion feature enabled.
* Support telnet and WebSocket, to enable both local and remote diagnostics with command line and browsers.


Contents
--------

English version has just been finished. If you would like to make it better, please check [here](https://github.com/alibaba/arthas/issues/51) and submit your pull request.

* [Installation](install-detail.md)
* [Quick start](quick-start.md)
* [Advanced usage](advanced-use.md)
* [Commands](commands.md)
* [User cases](https://github.com/alibaba/arthas/issues?q=label%3Auser-case)
* [Questions and answers](https://github.com/alibaba/arthas/issues?q=label%3Aquestion-answered)
* [Fork me at GitHub](https://github.com/alibaba/arthas)
* [CONTRIBUTING](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)
* [Release Notes](release-notes.md)
