# profiler

[`profiler` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-profiler)

::: tip
Generate a flame graph using [async-profiler](https://github.com/jvm-profiling-tools/async-profiler)
:::

The `profiler` command supports generating flame graph for application hotspots.

The basic usage of the `profiler` command is `profiler action [actionArg]`

The arguments of `profiler` command basically keeps consistent with upstream project [async-profiler](https://github.com/async-profiler/async-profiler), you can refer to its README, Github Discussions and other documentations for further information of usage.

## Supported Options

|        Name | Specification                                                                    |
| ----------: | :------------------------------------------------------------------------------- |
|    _action_ | Action to execute                                                                |
| _actionArg_ | Attribute name pattern                                                           |
|        [i:] | sampling interval in ns (default: 10'000'000, i.e. 10 ms)                        |
|        [f:] | dump output to specified directory                                               |
|        [d:] | run profiling for specified seconds                                              |
|        [e:] | which event to trace (cpu, alloc, lock, cache-misses etc.), default value is cpu |

## Start profiler

```
$ profiler start
Started [cpu] profiling
```

::: tip
By default, the sample event is `cpu`. Other valid profiling modes can be specified with the `--event` parameter, see relevant contents below.
:::

## Get the number of samples collected

```
$ profiler getSamples
23
```

## View profiling status

```bash
$ profiler status
[cpu] profiling is running for 4 seconds
```

Can view which `event` and sampling time.

## View profiler memory usage

```
$ profiler meminfo
Call trace storage:   10244 KB
      Dictionaries:      72 KB
        Code cache:   12890 KB
------------------------------
             Total:   23206 KB
```

## Stop profiler

### Generating flame graph results

By default, the result file is `html` file in [Flame Graph](https://github.com/BrendanGregg/FlameGraph) format. You can also specify other format with the `-o` or `--format` parameter, including flat, traces, collapsed, flamegraph, tree, jfr:

```bash
$ profiler stop --format flamegraph
profiler output file: /tmp/test/arthas-output/20211207-111550.html
OK
```

When extension of filename in `--file` parameter is `html` or `jfr`, the output format can be infered. For example, `--file /tmp/result.html` will generate flamegraph automatically.

## View profiler results under arthas-output via browser

By default, arthas uses port 3658, which can be opened: [http://localhost:3658/arthas-output/](http://localhost:3658/arthas-output/) View the `arthas-output` directory below Profiler results:

![](/images/arthas-output.jpg)

Click to view specific results:

![](/images/arthas-output-svg.jpg)

::: tip
If using the chrome browser, may need to be refreshed multiple times.
:::

## Profiler supported events

Under different platforms and different OSs, the supported events are different. For example, under macos:

```bash
$ profiler list
Basic events:
  cpu
  alloc
  lock
  wall
  itimer
```

Under linux

```bash
$ profiler list
Basic events:
  cpu
  alloc
  lock
  wall
  itimer
Java method calls:
  ClassName.methodName
Perf events:
  page-faults
  context-switches
  cycles
  instructions
  cache-references
  cache-misses
  branch-instructions
  branch-misses
  bus-cycles
  L1-dcache-load-misses
  LLC-load-misses
  dTLB-load-misses
  rNNN
  pmu/event-descriptor/
  mem:breakpoint
  trace:tracepoint
  kprobe:func
  uprobe:path
```

If you encounter the permissions/configuration issues of the OS itself and then missing some events, you can refer to the [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) documentation.

You can use `check` action to check if a profiling event is available, this action receives the same format options with `start`.

You can use the `--event` parameter to specify the event to sample, for example, `alloc` event means heap memory allocation profiling:

```bash
$ profiler start --event alloc
```

## Resume sampling

```bash
$ profiler resume
Started [cpu] profiling
```

The difference between `start` and `resume` is: `start` will clean existing result of last profiling before starting, `resume` will retain the existing result and add result of this time to it.

You can verify the number of samples by executing `profiler getSamples`.

## Dump action

```bash
$ profiler dump
OK
```

The `dump` action saves profiling result to default file or specified file, but profiling will continue. That means if you start profiling and dump after 5 seconds, then dump after 2 seconds again, you will get 2 result files, the first one contains profiling result of 0\~5 seconds and the second one contains that of 0\~7 seconds.

## Use `execute` action to execute complex commands

For example, start sampling:

```bash
profiler execute 'start,framebuf=5000000'
```

Stop sampling and save to the specified file:

```bash
profiler execute 'stop,file=/tmp/result.html'
```

Specific format reference: [arguments.cpp](https://github.com/async-profiler/async-profiler/blob/v2.9/src/arguments.cpp#L52)

## View all supported actions

```bash
$ profiler actions
Supported Actions: [resume, dumpCollapsed, getSamples, start, list, version, execute, meminfo, stop, load, dumpFlat, dump, actions, dumpTraces, status, check]
```

## View version

```bash
$ profiler version
Async-profiler 2.9 built on May  8 2023
Copyright 2016-2021 Andrei Pangin
```

## Configure Java stack depth

You can use `-j` or `--jstackdepth` option to configure maximum Java stack depth. This option will be ignored if value is greater than default 2048. This option is useful when you don't want to see stacks that are too deep. Below is usage example:

```bash
profiler start -j 256
```

## Profiling different threads separately

You can use `-t` or `--threads` flag option to profile different threads separately, each stack trace will end with a frame that denotes a single thread.

```bash
profiler start -t
```

## Configure include/exclude to filter data

If the application is complex and generates a lot of content, and you want to focus on only part of stack traces, you can filter stack traces by `--include/--exclude`. `--include` defines the name pattern that must be present in the stack traces, while `--exclude` is the pattern that must not occur in any of stack traces in the output.A pattern may begin or end with a star `*` that denotes any (possibly empty) sequence of characters. such as

```bash
profiler stop --include'java/*' --include 'com/demo/*' --exclude'*Unsafe.park*'
```

> Both `--include/--exclude` support being set multiple times, but need to be configured at the end of the command line. You can also use short parameter format `-I/-X`.
> Note that `--include/--exclude` only supports configuration at `stop` action or `start` action with `-d`/`--duration` parameter, otherwise it will not take effect.

## Specify execution time

For example, if you want the profiler to automatically end after 300 seconds, you can specify it with the `-d`/`--duration` parameter in collect action:

```bash
profiler collect --duration 300
```

## Generate jfr format result

> Note that jfr only supports configuration at `start`. If it is specified at `stop`, it will not take effect.

```
profiler start --file /tmp/test.jfr
profiler start -o jfr
```

The `file` parameter supports some variables:

- Timestamp: `--file /tmp/test-%t.jfr`
- Process ID: `--file /tmp/test-%p.jfr`

The generated results can be viewed with tools that support the jfr format. such as:

- JDK Mission Control: https://github.com/openjdk/jmc
- JProfiler: https://github.com/alibaba/arthas/issues/1416

## Control details in result

The `-s` parameter will use simple name instead of Fully qualified name, e.g. `MathGame.main` instead of `demo.MathGame.main`. The `-g` parameter will use method signatures instead of method names, e.g. `demo.MathGame.main([Ljava/lang/String;)V` instead of `demo.MathGame.main`. There are many parameters related to result format details, you can refer to [async-profiler README](https://github.com/async-profiler/async-profiler#readme) and [async-profiler Github Discussions](https://github.com/async-profiler/async-profiler/discussions) and other information.

For example, in command below, `-s` use simple name for Java class, `-g` show method signatures, `-a` will annotate Java methods, `-l` will prepend library names for native method, `--title` specify a title for flame graph page, `--minwidth` will skip frames smaller than 15% in flame graph, `--reverse` will generate stack-reversed FlameGraph / Call tree.

```
profiler stop -s -g -a -l --title <flametitle> --minwidth 15 --reverse
```

## The 'unknown' in profiler result

- https://github.com/jvm-profiling-tools/async-profiler/discussions/409

## Config locks/allocations profiling threshold

When profiling in locks or allocations event, you can use `--lock` or `--alloc` to config thresholds, for example:

```bash
profiler start -e lock --lock 10ms
profiler start -e alloc --alloc 2m
```

will profile contended locks longer than 10ms (default unit is ns if no unit is specified), or profile allocations with 2m BYTES interval.

## Config JFR chunks

When using JFR as output format, you can use `--chunksize` or `--chunktime` to config approximate size (in bytes, default value is 100MB) and time limits (default value is 1 hour) for a single JFR chunk. For example:

```bash
profiler start -f profile.jfr --chunksize 100m --chunktime 1h
```

## Group threads by scheduling policy

You can use `--sched` flag option to group threads in output by Linux-specific scheduling policy: BATCH/IDLE/OTHER, for example:

```bash
profiler start --sched
```

The second line from bottom in flamegraph represent the scheduling policy.

## Build allocation profile from live objects only

Use `--live` flag option to retain allocation samples with live objects only (object that have not been collected by the end of profiling session). Useful for finding Java heap memory leaks.

```bash
profiler start --live
```

## Config method of collecting C stack frames

Use `--cstack MODE` to config how to walk native frames (C stack). Possible modes are fp (Frame Pointer), dwarf (DWARF unwind info), lbr (Last Branch Record, available on Haswell since Linux 4.1), and no (do not collect C stack).

By default, C stack is shown in cpu, itimer, wall-clock and perf-events profiles. Java-level events like alloc and lock collect only Java stack.

```bash
profiler --cstack fp
```

The command above will collection Frame Pointer of C stacks.

## Begin or end profiling when FUNCTION is executed

Use `--begin function` and `--end function` to automatically start/stop profiling when the specified native function is executed. Its main purpose is to profile certain JVM phases like GC and Safepoint pauses. You should use native function name defined in a JVM implement, for example `SafepointSynchronize::begin` and `SafepointSynchronize::end` in HotSpot JVM.

### Time-to-safepoint profiling

The `--ttsp` option is an alias for `--begin SafepointSynchronize::begin --end RuntimeService::record_safepoint_synchronized`. It is not a separate event type, but rather a constraint. Whatever event type you choose (e.g. cpu or wall), the profiler will work as usual, except that only events between the safepoint request and the start of the VM operation will be recorded.

```bash
profiler start --begin SafepointSynchronize::begin --end RuntimeService::record_safepoint_synchronized
profiler --ttsp
```

## Use events from profiler for Java Flight Recording

Use `--jfrsync CONFIG` to start Java Flight Recording with the given configuration synchronously with the profiler. The output .jfr file will include all regular JFR events, except that execution samples will be obtained from async-profiler. This option implies -o jfr.

`CONFIG` can be `profile`, means using the predefined JFR config "profile" in `$JAVA_HOME/lib/jfr/`, or full path of a JFR configuration file (.jfc), this value has the same format with [settings option of JFR.start](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jcmd.html).

For example, command below use "profile" config of JFR:

```bash
profiler start -e cpu --jfrsync profile -f combined.jfr
```

## Run profiler in a loop

Use `--loop TIME` to run profiler in a loop (continuous profiling). The argument is either a clock time (hh:mm:ss) or a loop duration in seconds, minutes, hours, or days. Make sure the filename includes a timestamp pattern, or the output will be overwritten on each iteration. The command below will run profiling endlessly and save records of each hour to a jfr file.

```bash
profiler start --loop 1h -f /var/log/profile-%t.jfr
```

## `--timeout` option

This option specifies the time when profiling will automatically stop. The format is the same as in loop: it is either a wall clock time (12:34:56) or a relative time interval (2h).

Both `--loop` and `--timeout` are used for `start` action but not for `collect` action, for further information refer to [async-profiler Github Discussions](https://github.com/async-profiler/async-profiler/discussions/789).
