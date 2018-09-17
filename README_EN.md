## Arthas

![arthas](site/src/site/sphinx/arthas.png)

`Arthas` is an Java Diagnostic tool open sourced by Alibaba.

Arthas can help developer trouble-shooting production issues for Java applications without modifying your code or restating your server.

### Background

In production system, sometimes the networks is unreachable from local development environment. If you encounter some issues in production system, it is impossible to use IDE to debug the application remotely. More importantly, debugging in production is unacceptable, because it will suspend all the threads, which leads to business unavailability. 

Some may think of reproducing the same issue on the test/staging environment, however, some tricky issue either can hardly be reproduced on a different environment, or even disappeared once restarted. 

Thinking of adding some logs to your code? You have to go through test, staging, and then on to production. Time is money! That is a lot inefficient! Besides, the issue may not be reproducible once restart your JVM, as described above.

Arthas is born to solve these issues. You can trouble-shoot your production issue on-the-fly. No JVM restart, no additional code changes. Arthas works as an observer, which will never suspend your existing threads.

### Key features

* check whether a class is loaded? And whether is class loaded from? (Useful for trouble-shooting jar file conflicts)
* decompile a class to ensure the code is running as expected.
* view classloader statistics, e.g. how may classloaders are there? how many class is loaded per classloader? What is the classloader hierarchy? Is there possible classloader leaks?
* view the method invocation details, e.g. method parameter, return object, thrown exception, and etc.
* check the stack trace of specified method invocation. This is useful when you would like to know who is calling your method?
* trace the method invocation to find slow sub-invocations.
* monitoring your system metrics, thread states and cpu usage, gc statistics, and etc.
* command line interactive mode, with auto completed feature enabled.
* telnet and websocket support, which enables both local and remote diagnostics with command line and browsers.

### Documentation

* [User manual](https://alibaba.github.io/arthas/)
* [Installation](https://alibaba.github.io/arthas/install-detail.html)
* [Quick start](https://alibaba.github.io/arthas/quick-start.html)
* [Advanced usage](https://alibaba.github.io/arthas/advanced-use.html)
* [Questions and answers](https://github.com/alibaba/arthas/labels/question-answered)
* [How to contribute](https://github.com/alibaba/arthas/blob/master/CONTRIBUTING.md)


### Feature Showcase

#### Dashboard

* https://alibaba.github.io/arthas/dashboard

![dashboard](site/src/site/sphinx/_static/dashboard.png)


#### Web Console

* https://alibaba.github.io/arthas/web-console

![web console](site/src/site/sphinx/_static/web-console-local.png)