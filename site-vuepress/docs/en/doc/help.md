help
===

show help message, the command can show all the commands that current Arthas server supports,or you can use the command to show the detail usage of another command.

> [help command] equals [command -help],both is to show the detail usage of one command.

### Options

|    Name | Specification                                             |
| ------: | :-------------------------------------------------------- |
|         | show all the commands that current Arthas server supports |
| [name:] | show the detail usage of one command                      |

### Usage

```bash
$ help
 NAME         DESCRIPTION
 help         Display Arthas Help
 auth         Authenticates the current session
 keymap       Display all the available keymap for the specified connection.
 sc           Search all the classes loaded by JVM
 sm           Search the method of classes loaded by JVM
 classloader  Show classloader info
 jad          Decompile class
 getstatic    Show the static field of a class
 monitor      Monitor method execution statistics, e.g. total/success/failure count, average rt, fail rate, etc.
 stack        Display the stack trace for the specified class and method
 thread       Display thread info, thread stack
 trace        Trace the execution time of specified method invocation.
 watch        Display the input/output parameter, return object, and thrown exception of specified method invocation
 tt           Time Tunnel
 jvm          Display the target JVM information
 perfcounter  Display the perf counter information.
 ognl         Execute ognl expression.
 mc           Memory compiler, compiles java files into bytecode and class files in memory.
 redefine     Redefine classes. @see Instrumentation#redefineClasses(ClassDefinition...)
 retransform  Retransform classes. @see Instrumentation#retransformClasses(Class...)
 dashboard    Overview of target jvm's thread, memory, gc, vm, tomcat info.
 dump         Dump class byte array from JVM
 heapdump     Heap dump
 options      View and change various Arthas options
 cls          Clear the screen
 reset        Reset all the enhanced classes
 version      Display Arthas version
 session      Display current session information
 sysprop      Display, and change the system properties.
 sysenv       Display the system env.
 vmoption     Display, and update the vm diagnostic options.
 logger       Print logger info, and update the logger level
 history      Display command history
 cat          Concatenate and print files
 base64       Encode and decode using Base64 representation
 echo         write arguments to the standard output
 pwd          Return working directory name
 mbean        Display the mbean information
 grep         grep command for pipes.
 tee          tee command for pipes.
 profiler     Async Profiler. https://github.com/jvm-profiling-tools/async-profiler
 stop         Stop/Shutdown Arthas server and exit the console.
 
 
```

```bash
 $ help dashboard
  USAGE:
   dashboard [-h] [-i <value>] [-n <value>]

 SUMMARY:
   Overview of target jvm's thread, memory, gc, vm, tomcat info.

 EXAMPLES:
   dashboard
   dashboard -n 10
   dashboard -i 2000

 WIKI:
   https://arthas.aliyun.com/doc/dashboard

 OPTIONS:
 -h, --help                              this help
 -i, --interval <value>                  The interval (in ms) between two executions, default is 5000 ms.
 -n, --number-of-execution <value>       The number of times this command will be executed.
```

