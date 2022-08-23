# All Commands

## jvm - related

- [dashboard](dashboard.md) - dashboard for the system's real-time data
- [getstatic](getstatic.md) - examine class's static properties
- [heapdump](heapdump.md) - dump java heap in hprof binary format, like `jmap`
- [jvm](jvm.md) - show JVM information
- [logger](logger.md) - print the logger information, update the logger level
- [mbean](mbean.md) - show Mbean information
- [memory](memory.md) - show JVM memory information
- [ognl](ognl.md) - execute ognl expression
- [perfcounter](perfcounter.md) - show JVM Perf Counter information
- [sysenv](sysenv.md) — view system environment variables
- [sysprop](sysprop.md) - view/modify system properties
- [thread](thread.md) - show java thread information
- [vmoption](vmoption.md) - view/modify the vm diagnostic options.
- [vmtool](vmtool.md) - jvm tool, getInstances in jvm, forceGc

## class/classloader - related

- [classloader](classloader.md) - check the inheritance structure, urls, class loading info for the specified class; using classloader to get the url of the resource e.g. `java/lang/String.class`
- [dump](dump.md) - dump the loaded classes in byte code to the specified location
- [jad](jad.md) - decompile the specified loaded classes
- [mc](mc.md) - Memory compiler, compiles `.java` files into `.class` files in memory
- [redefine](redefine.md) - load external `*.class` files and re-define it into JVM
- [retransform](retransform.md) - load external `*.class` files and retransform it into JVM
- [sc](sc.md) - check the info for the classes loaded by JVM
- [sm](sm.md) - check methods info for the loaded classes

## monitor/watch/trace - related

::: warning
**Attention**: commands here are taking advantage of byte-code-injection, which means we are injecting some [aspects](https://en.wikipedia.org/wiki/Aspect-oriented_programming) into the current classes for monitoring and statistics purpose. Therefore, when using it for online troubleshooting in your production environment, you'd better **explicitly specify** classes/methods/criteria, and remember to remove the injected code by `stop` or `reset`.
:::

- [monitor](monitor.md) - monitor method execution statistics
- [stack](stack.md) - display the stack trace for the specified class and method
- [trace](trace.md) - trace the execution time of specified method invocation
- [tt](tt.md) - time tunnel, record the arguments and returned value for the methods and replay
- [watch](watch.md) - display the input/output parameter, return object, and thrown exception of specified method invocation

## profiler/flame graph

- [profiler](profiler.md) - use [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) to generate flame graph
- [jfr](jfr.md) - dynamic opening and closing of jfr recordings

## authentication

- [auth](auth.md) - authentication

## options

- [options](options.md) - check/set Arthas global optionss

## pipe

Arthas provides `pipe` to process the result returned from commands further, e.g. `sm java.lang.String * | grep 'index'`. Commands supported in `pipe`:

- [grep](grep.md)- filter the result with the given keyword
- plaintext - remove the ANSI color
- wc - count lines

## async jobs

[async](async.md) can be handy when a problem is hardly to reproduce in the production environment, e.g. one `watch` condition may happen only once in one single day.

- job control - use `>` to redirect result into the log file, use `&` to put the job to the background. Job keeps running even if the session is disconnected (the session lifecycle is 1 day by default)
- jobs - list all jobs
- kill - forcibly terminate the job
- fg - bring the suspend job to the foreground
- bg - put the job to run in the background

## Basic Arthas Commands

- [base64](base64.md) - Encode and decode using Base64 representation.
- [cat](cat.md) - Concatenate and print files
- [cls](cls.md) - clear the screen
- [echo](echo.md) - write arguments to the standard output
- [grep](grep.md) - Pattern searcher
- [help](help.md) - display Arthas help
- [history](history.md) - view command history
- [keymap](keymap.md) - keymap for Arthas keyboard shortcut
- [pwd](pwd.md) - Return working directory name
- [quit/exit](quit.md) - exit the current Arthas session, without effecting other sessions
- [reset](reset.md) - reset all the enhanced classes. All enhanced classes will also be reset when Arthas server is closed by `stop`
- [session](session.md) - display current session information
- [stop](stop.md) - terminate the Arthas server, all Arthas sessions will be destroyed
- [tee](tee.md) - Copies standard input to standard output, making a copy in zero or more files.
- [version](version.md) - print the version for the Arthas attached to the current Java process
