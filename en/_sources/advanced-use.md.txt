Advanced Usage
==============

## Basic

- help
- cls - clear out the current screen
- session - check details of the current session
- [reset](reset.md) - reset all empowered classes
- version - print the version of the Arthas attaching to the current target process
- quit/exit - exit the current Arthas client without affecting other clients
- shutdown - terminate the Arthas server and all clients

## JVM-related

* [dashboard](dashboard.md) - real-time dashboard for the current system
* [thread](thread.md) - thread profile
* [jvm](jvm.md) - JVM profile
* [sysprop](sysprop.md) - check or modify JVM system properties
* **New!** [getstatic](getstatic.md) - check the static properties of some class

## class/classloader - related


* [sc](sc.md) - methods' profile of the classes loaded by JVM 
* [sm](sm.md) - methods' profile of the loaded classes
* [dump](dump.md) - dump out the byte code of the loaded class to specified location
* [redefine](redefine.md) - load external `*.class` files and re-define the JVM
* [jad](jad.md) - de-compile the specified loaded classes
* [classloader](classloader.md) - check the inheritance structure, urls, class loading info of class cloader; using classloader to getResource

## monitor/watch/trace - related

> **Attention**: commands here are taking advantage of `byte code injection`, which means we are using [AOP](https://en.wikipedia.org/wiki/Aspect-oriented_programming) to monitor and analyze the classes. So when using it for online troubleshooting, you'd better *explicitly specifically* specify the classes and also remember to remove the injected code by `shutdown` or `reset` (for specific classes). 

* [monitor](monitor.md)
* [watch](watch.md)
* [trace](trace.md) - track the call stack trace and collect the time cost for each method call
* [stack](stack.md) - print the call stack trace of the current method
* [tt](tt.md) - record the arguments and returned value for the methods, history included

## options

* [options](options.md) - check or set Arthas global options


## pipe

`pipe` is supported in Arthas, e.g. `sm org.apache.log4j.Logger | grep <init>`

* grep 
* plaintext - remove the color
* wc

## async in background

[async](async.md) will be a great help, when the `incident` seldom occurs and you are `[watch](watch.md)`ing it. 


* jobs - list all jobs
* kill - forcibly terminate the job
* fg - bring the paused job back to the front
* bg - put the paused job to the background
* tips - a) use `>` to redirect the output; b) use `&` to put the job to the background; c) disconnecting the session will not influence the job (the default life is 1 day)

## Web Console

Using websocket to connect Arthas

* [Web Console](web-console.md)

## Others

* [log the output](save-log.md)
* [batch](batch-support.md)
* [how to use ognl](https://github.com/alibaba/arthas/issues/11)



