::: tip
Java Flight Recorder (JFR) is a tool for collecting diagnostic and profiling data about a running Java application. It is integrated into the Java Virtual Machine (JVM) and causes almost no performance overhead, so it can be used even in heavily loaded production environments.
:::

The [jfr command](https://arthas.aliyun.com/en/doc/jfr.html) supports starting and stopping JFR recordings during dynamic program running. Recording collects data about _events_. Events occur in the JVM or the Java application at a specific point in time. Each event has a name, a time stamp, and an optional _payload_. The payload is the data associated with an event, for example, the CPU usage, the Java heap size before and after the event, the thread ID of the lock holder, and so on.

The basic usage of the `jfr` command is`jfr cmd [actionArg]`

> Note: jfr is supported only after the 8u262 version of jdk8

## Start jfr recording

`jfr start`{{execute T2}}

```
$ jfr start
Started recording 1. No limit specified, using maxsize=250MB as default.
```

::: tip
The default JFR record is started.
:::

Start the JFR recording, specify the recording name, duration, file saving path.

`jfr start -n myRecording --duration 60s -f /tmp/myRecording.jfr`{{execute T2}}

## View jfr recordings status

The default is to view all JFR recordings.

`jfr status`{{execute T2}}

View the records of the specified recording ID.

`jfr status -r 1`{{execute T2}}

View recordings in a specified state.

`jfr status --state closed`{{execute T2}}

## dump jfr recording

Specifies the record output path.

`$ jfr dump -r 1 -f /tmp/myRecording1.jfr`{{execute T2}}

The file output path is not specified. By default, it is saved to the `arthas-output` directory

`jfr dump -r 1`{{execute T2}}

## Stop jfr recording

No recording output path is specified, default is saved to `arthas-output` directory.

`jfr stop -r 1`{{execute T2}}

> notice: A recording can only be stopped once.

You can also specify the record output path.

## View JFR recording results under arthas-output via browser

By default, arthas uses http port 8563 , which can be opened:[http://localhost:8563/arthas-output/](http://localhost:8563/arthas-output/) View the `arthas-output` directory below JFR recording results:

![](/images/arthas-output-recording.png)

The resulting results can be viewed with tools that support the JFR format. Such as:

- JDK Mission Control: https://github.com/openjdk/jmc
