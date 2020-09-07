
> Generate a flame graph using [async-profiler](https://github.com/jvm-profiling-tools/async-profiler)

The `profiler` command supports generate flame graph for application hotspots.

The basic usage of the `profiler` command is `profiler action [actionArg]`

### Supported Options

|Name|Specification|
|---:|:---|
|*action*|Action to execute|
|*actionArg*|Attribute name pattern|
|[i:]|sampling interval in ns (default: 10'000'000, i.e. 10 ms)|
|[f:]|dump output to specified directory|
|[d:]|run profiling for specified seconds|
|[e:]|which event to trace (cpu, alloc, lock, cache-misses etc.), default value is cpu|
 
### View all supported actions

`profiler actions`{{execute T2}}

```bash
$ profiler actions
Supported Actions: [resume, dumpCollapsed, getSamples, start, list, execute, version, stop, load, dumpFlat, actions, dumpTraces, status]
```


### View version

`profiler version`{{execute T2}}

```bash
$ profiler version
Async-profiler 1.6 built on Sep  9 2019
Copyright 2019 Andrei Pangin
```

### Start profiler

`profiler start -e itimer`{{execute T2}}

```
$ profiler start
Started [cpu] profiling
```

> By default, the sample event is `cpu`. Can be specified with the `--event` parameter.
> Since katacoda environment doesn't support `perf_events`，here use `-e itimer` to specify event to be `itimer`


### Get the number of samples collected

`profiler getSamples`{{execute T2}}

```
$ profiler getSamples
23
```

### View profiler status

`profiler status`{{execute T2}}

```bash
$ profiler status
[cpu] profiling is running for 4 seconds
```

Can view which `event` and sampling time.

### Stop profiler

#### Generate svg format results

`profiler stop`{{execute T2}}

```
$ profiler stop
profiler output file: /tmp/demo/arthas-output/20191125-135546.svg
OK
```

By default, the generated results are saved to the `arthas-output` directory under the application's `working directory`. The output result path can be specified by the `--file` parameter. such as:

`profiler stop --file /tmp/output.svg`{{execute T2}}

```bash
$ profiler stop --file /tmp/output.svg
profiler output file: /tmp/output.svg
OK
```

#### Generating html format results

By default, the result file is `svg` format. If you want to generate the `html` format, you can specify it with the `--format` parameter:

`profiler stop --format html`{{execute T2}}

```bash
$ profiler stop --format html
profiler output file: /tmp/test/arthas-output/20191125-143329.html
OK
```

Or use the file name name format in the `--file` parameter. For example, `--file /tmp/result.html`.

`profiler stop --file /tmp/result.html`{{execute T2}}

### View profiler results under arthas-output via browser

By default, arthas uses http port 8563, which can be opened: https://[[HOST_SUBDOMAIN]]-8563-[[KATACODA_HOST]].environments.katacoda.com/arthas-output/ View the `arthas-output` directory below Profiler results:

![](https://arthas.aliyun.com/doc/_images/arthas-output.jpg)

Click to view specific results:

![](https://arthas.aliyun.com/doc/_images/arthas-output-svg.jpg)

> If using the chrome browser, may need to be refreshed multiple times.

### Profiler supported events

`profiler list`{{execute T2}}

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
Perf events:
  page-faults
  context-switches
  cycles
  instructions
  cache-references
  cache-misses
  branches
  branch-misses
  bus-cycles
  L1-dcache-load-misses
  LLC-load-misses
  dTLB-load-misses
  mem:breakpoint
  trace:tracepoint
```

If you encounter the permissions/configuration issues of the OS itself and then missing some events, you can refer to the [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) documentation.

You can use the `--event` parameter to specify the event to sample, such as sampling the `alloc` event:

`profiler start --event alloc`{{execute T2}}

```bash
$ profiler start --event alloc
```


### Resume sampling

`profiler resume`{{execute T2}}

```bash
$ profiler resume
Started [cpu] profiling
```

The difference between `start` and `resume` is: `start` is the new start sampling, `resume` will retain the data of the last `stop`.

You can verify the number of samples by executing `profiler getSamples`.


### Use `execute` action to execute complex commands

`profiler execute 'start,framebuf=5000000'`{{execute T2}}

For example, start sampling:  

```bash
profiler execute 'start,framebuf=5000000'
```

Stop sampling and save to the specified file:

`profiler execute 'stop,file=/tmp/result.svg'`{{execute T2}}

```bash
profiler execute 'stop,file=/tmp/result.svg'
```

Specific format reference: [arguments.cpp](https://github.com/jvm-profiling-tools/async-profiler/blob/v1.8.1/src/arguments.cpp#L50)
