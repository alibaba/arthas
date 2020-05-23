Advanced Usage
==============

## Basic

* help - display Arthas help
* cls - clear the screen
* [cat](cat.md) - Concatenate and print files
* [echo](echo.md) - write arguments to the standard output
* [grep](grep.md) - Pattern searcher
* [tee](tee.md) - Copies standard input to standard output, making a copy in zero or more files.
* [pwd](pwd.md) - Return working directory name
* session - display current session information
* [reset](reset.md) - reset all the enhanced classes. All enhanced classes will also be reset when Arthas server is closed by `stop`
* version - print the version for the Arthas attached to the current Java process
* history - view command history
* quit/exit - exit the current Arthas session, without effecting other sessions
* stop - terminate the Arthas server, all Arthas sessions will be destroyed
* [keymap](keymap.md) - keymap for Arthas keyboard shortcut

## JVM

* [dashboard](dashboard.md) - dashboard for the system's real-time data
* [thread](thread.md) - show java thread information
* [jvm](jvm.md) - show JVM information
* [sysprop](sysprop.md) - view/modify system properties
* [sysenv](sysenv.md) — view system environment variables
* [vmoption](vmoption.md) - view/modify the vm diagnostic options.
* [perfcounter](perfcounter.md) - show JVM Perf Counter information
* [logger](logger.md) - print the logger information, update the logger level
* [getstatic](getstatic.md) - examine class's static properties
* [ognl](ognl.md) - execute ongl expression
* [mbean](mbean.md) - show Mbean information
* [heapdump](heapdump.md) - dump java heap in hprof binary format, like `jmap`

## class/classloader

* [sc](sc.md) - check the info for the classes loaded by JVM 
* [sm](sm.md) - check methods info for the loaded classes
* [jad](jad.md) - decompile the specified loaded classes
* [mc](mc.md) - Memory compiler, compiles `.java` files into `.class` files in memory
* [redefine](redefine.md) - load external `*.class` files and re-define it into JVM
* [dump](dump.md) - dump the loaded classes in byte code to the specified location
* [classloader](classloader.md) - check the inheritance structure, urls, class loading info for the specified class; using classloader to get the url of the resource e.g. `java/lang/String.class`

## monitor/watch/trace - related

> **Attention**: commands here are taking advantage of byte-code-injection, which means we are injecting some [aspects](https://en.wikipedia.org/wiki/Aspect-oriented_programming) into the current classes for monitoring and statistics purpose. Therefore, when using it for online troubleshooting in your production environment, you'd better **explicitly specify** classes/methods/criteria, and remember to remove the injected code by `stop` or `reset`. 

* [monitor](monitor.md) - monitor method execution statistics
* [watch](watch.md) - display the input/output parameter, return object, and thrown exception of specified method invocation
* [trace](trace.md) - trace the execution time of specified method invocation
* [stack](stack.md) - display the stack trace for the specified class and method
* [tt](tt.md) - time tunnel, record the arguments and returned value for the methods and replay

## options

* [options](options.md) - check/set Arthas global options


## profiler/flame graph

* [profiler](profiler.md) - use [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) to generate flame graph

## pipe

Arthas provides `pipe` to process the result returned from commands further, e.g. `sm java.lang.String * | grep 'index'`. Commands supported in `pipe`:

* grep - filter the result with the given keyword
* plaintext - remove the ANSI color
* wc - count lines

## async in background

[async](async.md) can be handy when a problem is hardly to reproduce in the production environment, e.g. one `watch` condition may happen only once in one single day.

* job control - use `>` to redirect result into the log file, use `&` to put the job to the background. Job keeps running even if the session is disconnected (the session lifecycle is 1 day by default)
* jobs - list all jobs
* kill - forcibly terminate the job
* fg - bring the suspend job to the foreground
* bg - put the job to run in the background

## Web Console

Arthas supports living inside a browser. The communication between arthas and browser is via websocket.

* [Web Console](web-console.md)

## Start as a Java Agent

* [Start as a Java Agent](agent.md)

## User data report

After the `3.1.4` version, arthas support user data report.

At startup, use the `stat-url` option, such as: `./as.sh --stat-url 'http://192.168.10.11:8080/api/stat'`

There is a sample data report in the tunnel server that users can implement on their own.

[StatController.java](https://github.com/alibaba/arthas/blob/master/tunnel-server/src/main/java/com/alibaba/arthas/tunnel/server/app/web/StatController.java)

## Other features

* [Async support](async.md)
* [log the output](save-log.md)
* [batch](batch-support.md)
* [how to use ognl](https://github.com/alibaba/arthas/issues/11)



