# jfr

[`jfr` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-jfr)

::: tip
Java Flight Recorder (JFR) is a tool for collecting diagnostic and profiling data about a running Java application. It is integrated into the Java Virtual Machine (JVM) and causes almost no performance overhead, so it can be used even in heavily loaded production environments.
:::

The `jfr` command supports starting and stopping JFR recordings during dynamic program running. Recording collects data about _events_. Events occur in the JVM or the Java application at a specific point in time. Each event has a name, a time stamp, and an optional _payload_. The payload is the data associated with an event, for example, the CPU usage, the Java heap size before and after the event, the thread ID of the lock holder, and so on.

The basic usage of the `jfr` command is`jfr cmd [actionArg]`

> Note: jfr is supported only after the 8u262 version of jdk8

### Supported Options

|          Name | Specification                                                                                |
| ------------: | :------------------------------------------------------------------------------------------- |
|         _cmd_ | Command to execute, support【start，status，dump，stop】                                     |
|   _actionArg_ | Attribute name pattern                                                                       |
|          [n:] | Name of recording                                                                            |
|          [r:] | Recording id                                                                                 |
| [dumponexit:] | When the program exits, whether to dump the .jfr file. (boolean false)                       |
|          [d:] | Duration of recording, i.e. 60s, 2m, 5h, 3d. default no delay                                |
|   [duration:] | Duration of recording, default forever.                                                      |
|          [s:] | Server-side template, The default is default.jfc located at `$JAVA_HOME/lib/jfr/default.jfc` |
|          [f:] | Resulting recording filename                                                                 |
|     [maxage:] | Maximum age of buffer data                                                                   |
|    [maxsize:] | Maximum size of buffers in bytes                                                             |
|      [state:] | Recording state                                                                              |

## Start jfr recording

```
$ jfr start
Started recording 1. No limit specified, using maxsize=250MB as default.
```

::: tip
The default JFR record is started.
:::

Start the JFR recording, specify the recording name, duration, file saving path.

```
$ jfr start -n myRecording --duration 60s -f /tmp/myRecording.jfr
Started recording 2. The result will be written to:
/tmp/myRecording.jfr
```

##View jfr recordings status

The default is to view all JFR recordings.

```bash
$ jfr status
Recording: recording=1 name=Recording-1 (running)
Recording: recording=2 name=myRecording duration=PT1M (closed)
```

View the records of the specified recording ID.

```bash
$ jfr status -r 1
Recording: recording=1 name=Recording-1 (running)
```

View recordings in a specified state.

```bash
$ jfr status --state closed
Recording: recording=2 name=myRecording duration=PT1M (closed)
```

## dump jfr recording

The `jfr dump`{{}} command will output the recordings from the start until the execution of the command to a JFR file, without stopping the recording.  
Specifies the record output path.

```bash
$ jfr dump -r 1 -f /tmp/myRecording1.jfr
Dump recording 1, The result will be written to:
/tmp/myRecording1.jfr
```

The file output path is not specified. By default, it is saved to the `arthas-output` directory

```bash
$ jfr dump -r 1
Dump recording 1, The result will be written to:
/tmp/test/arthas-output/20220819-200915.jfr
```

## Stop jfr recording

No recording output path is specified, default is saved to `arthas-output` directory.

```bash
$ jfr stop -r 1
Stop recording 1, The result will be written to:
/tmp/test/arthas-output/20220819-202049.jfr
```

> notice: A recording can only be stopped once.

You can also specify the record output path.

## View JFR recording results under arthas-output via browser

By default, arthas uses http port 8563 , which can be opened:[http://localhost:8563/arthas-output/](http://localhost:8563/arthas-output/) View the `arthas-output` directory below JFR recording results:

![](/images/arthas-output-recording.png)

The resulting results can be viewed with tools that support the JFR format. Such as:

- JDK Mission Control ： https://github.com/openjdk/jmc
