Arthas Documentation
===

**[中文文档/Chinese Docs](https://alibaba.github.io/arthas/)**

![arthas](arthas.png)

`Arthas` is an Java Diagnostic tool open sourced by Alibaba.

Arthas can help developer trouble-shooting production issues for Java applications without modifying your code or restating your server.

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


Contents
--------

English version has just been finished. If you would like to make it better, please check [here](https://github.com/alibaba/arthas/issues/51) and submit your PM.

* [Installation](install-detail.md)
* [Quick start](quick-start.md)
* [Advanced usage](advanced-use.md)
* [Commands](commands.md)
* [User cases](https://github.com/alibaba/arthas/issues?q=label%3Auser-case)
* [Release Notes](release-notes.md)
* [Questions and answers](https://github.com/alibaba/arthas/issues?q=label%3Aquestion-answered)
* [Fork me at GitHub](https://github.com/alibaba/arthas)
* [CONTRIBUTING](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)
