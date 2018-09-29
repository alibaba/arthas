Arthas Documentation
===

**[中文文档/Chinese Docs](https://alibaba.github.io/arthas/)**

![arthas](arthas.png)

**Arthas** is a Java diagnostic tool, open sourced by [Alibaba](https://github.com/alibaba).

Arthas help developers in troubleshooting without modifying and restarting online service.

### Background

More often than not, the production environments are **inaccessible**. Once there are issues online, there are two obvious difficulties making it *comparably impossible* to solve it online. First, it's hard to debug online service as what we normally do in IDE; second, debugging, updating and restarting (suspending/stopping) services in production is **not** acceptable. 

Apart from the impossibilities mentioned, still there are others as follows: 

- hard (even impossible) to **re-produce** the online issues in develop/test/staging environments without online data; 
- **re-producing** online issues can be impossible, if the issues are dependent upon the environments and can disappear after re-start;
- even adding logs to track down the issues for troubleshooting can be cumbersome (time-consuming) while the contradiction is issues online are always urgent;

**Arthas is here to solve these issues.**

Developers can start on-the-spot troubleshooting just online without code modifications and JVM restart. 

What developers can do with **Arthas**:

- online code checking/investigation;
- online call tracing;
- online call monitoring;
- online method call re-playing (re-producing);
- online code dynamic loading;

and [more...](commands.md)

With the help of **Arthas**, there is no service downtime for online **trouble-fixing** any more.

### You might wanna know

* check whether a class is loaded or where the class is loaded from to solve jar file conflicts;
* decompile a class to ensure the code is loaded as expected;
* check class loader statistics, e.g. the number of class loaders, the number of classes loaded per class loader, the class loader inheritance hierarchy, possible class loader leaks, etc.
* check the method invoking details, e.g. parameters, return values, exceptions, etc;
* check the stack trace of specified method invocation;
* trace the method invocation to locate the most time-consuming sub-callings;
* monitor method invocation statistics, e.g. QPS (Query Per Second), RT (return time), success rate, etc;
* monitor system metrics, thread states and CPU usage, GC statistics, etc;
* CLI (Command-Line user Interface) with auto-completion;
* local and remote diagnostics with CLI and browsers supported with Telnet and WebSocket.


### Others

If you would like to make the documentation better, please check [this issue](https://github.com/alibaba/arthas/issues/51) and submit your PM.

* [Installation](install-detail.md)
* [Quick start](quick-start.md)
* [Advanced usage](advanced-use.md)
* [Commands](commands.md)
* [User cases](https://github.com/alibaba/arthas/issues?q=label%3Auser-case)
* [Release Notes](release-notes.md)
* [Questions and answers](https://github.com/alibaba/arthas/issues?q=label%3Aquestion-answered)
* [Fork me at GitHub](https://github.com/alibaba/arthas)
* [CONTRIBUTING](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)
