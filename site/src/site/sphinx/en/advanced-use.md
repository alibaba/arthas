Advanced Usage
==============

## Basic

- help - show help info
- cls - clear out the current screen
- session - check details of the current session
- [reset](reset.md) - reset all injected/enhanced classes
- version - print the version of the Arthas attaching to the current target process
- quit/exit - exit the current Arthas client without affecting other clients
- shutdown - terminate the Arthas server and all clients

## JVM

* [dashboard](dashboard.md) - real-time dashboard for the current system
* [thread](thread.md) - thread profile
* [jvm](jvm.md) - JVM profile
* [sysprop](sysprop.md) - check or modify JVM system properties
* **New!** [getstatic](getstatic.md) :clap: - check the static properties of classes

## class/classloader


* [sc](sc.md) - check profiles of the classes loaded by JVM 
* [sm](sm.md) - check methods' profile
* [dump](dump.md) - dump out the byte code of the loaded classes to specified location
* [redefine](redefine.md) - load external `*.class` files and re-define the JVM-loaded classes
* [jad](jad.md) - de-compile the specified loaded classes
* [classloader](classloader.md) - check the inheritance structure, urls, class loading info of class cloader; using classloader to get the url of the resource e.g. `java/lang/String.class`

## monitor/watch/trace - related

> **Attention**: commands here are taking advantage of `byte code injection`, which means we are using [AOP](https://en.wikipedia.org/wiki/Aspect-oriented_programming) to monitor and analyze the classes. So when using it for online troubleshooting, you'd better *explicitly specifically* specify the classes and also remember to remove the injected code by `shutdown` or `reset`. 

* [monitor](monitor.md) - monitor the `class-pattern` & `method-pattern` matched methods' invoking traces
* [watch](watch.md) - watch/monitor methods in data aspect including `return values`, `exceptions` and `parameters`
* [trace](trace.md) - track the method calling trace along with the time cost for each call
* [stack](stack.md) - print the call stack trace of the current method in a persistent way
* [tt](tt.md) - record the arguments and returned value for the methods, history included

## options

* [options](options.md) - check or set Arthas global options


## pipe

`pipe` is supported in Arthas, e.g. `sm org.apache.log4j.Logger | grep <init>`

Commands supported in `pipe`:

* grep - filtering
* plaintext - remove the color
* wc - line counting

## async in background

[async](async.md) will be a great help, when the `incident` seldom occurs and you are [`watch`](watch.md)ing it. 


* jobs - list all jobs
* kill - forcibly terminate the job
* fg - bring the paused job back to the front
* bg - put the paused job to the background
* tips - a) use `>` to redirect the output; b) use `&` to put the job to the background; c) disconnecting the session will not influence the job (the default life is 1 day)

## Others

* [Web Console](web-console.md) - using websocket to connect Arthas
* [log the output](save-log.md)
* [batch](batch-support.md)
* [how to use ognl](https://github.com/alibaba/arthas/issues/11)



