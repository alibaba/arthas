### List the top n busiest threads with detailed stack trace

`thread -n 3`{{execute T2}}

- Without thread ID, including `[Internal]` means JVM internal thread, refer to the introduction of `dashboard` command.
- `cpuUsage` is the CPU usage of the thread during the sampling interval, consistent with the data of the `dashboard` command.
- `deltaTime` is the incremental CPU time of the thread during the sampling interval. If it is less than 1ms, it will be rounded and displayed as 0ms.
- `time` The total CPU time of thread.

**Note:** The thread stack is acquired at the end of the second sampling, which does not indicate that the thread is
processing the same task during the sampling interval. It is recommended that the interval time should not be too long.
The larger the interval time, the more inaccurate.

You can try to specify different intervals according to the specific situation and observe the output results.

#### List first page threads' info when no options provided

By default, they are arranged in descending order of CPU increment time, and only the first page of data is displayed.

`thread`{{execute T2}}

### thread id, show the running stack for the target thread

View the stack of thread ID 16:

`thread 16`{{execute T2}}

### thread -b, locate the thread bocking the others

In some occasions, we experience the whole application is stuck because there’s one particular thread hold one lock that other threads are relying on. To diagnose such an issue, Arthas provides thread -b to find the problematic thread in one single command.

`thread -b`{{execute T2}}

**Note**: By now Arthas only supports to locate the thread blocked by synchronzied, while `java.util.concurrent.Lock` is not supported yet.

### thread -i, specify the sampling interval

- `thread -i 1000`: Count the thread cpu time of the last 1000ms.

`thread -i 1000`{{execute T2}}

- `thread -n 3 -i 1000`: List the 3 busiest thread stacks in 1000ms.

`thread -n 3 -i 1000`{{execute T2}}

### thread –state , view the special state theads

`thread --state WAITING`{{execute T2}}
