mbean
=======

[`mbean` online tutorial](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-mbean)

> show Mbean information

This command can show or monitor Mbean attribute information. 

### Parameters

|Name|Specification|
|---:|:---|
|*name-pattern*|pattern for the Mbean name|
|*attribute-pattern*|pattern for the attribute name|
|[m]|show meta information|
|[i:]|specify the interval to refresh attribute value (ms)|
|[n:]|execution times|
|[E]|turn on regex matching while the default mode is wildcard matching. Only effect on the attribute name|

### Usage

show all Mbean names:

```bash
mbean
```

show meta data of Mbean:

```bash
mbean -m java.lang:type=Threading
```

show attributes of Mbean:

```bash
mbean java.lang:type=Threading 
```


Mbean name support wildcard matcher:

```bash
mbean java.lang:type=Th*
```

> Notes：ObjectName matching rules differ from normal wildcards, Reference resources：[javax.management.ObjectName](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html?is-external=true)

Wildcards match specific attributes:

```bash
mbean java.lang:type=Threading *Count
```

Switch to regular matching using the `-E` command:

```bash
mbean -E java.lang:type=Threading PeakThreadCount|ThreadCount|DaemonThreadCount
```

Real-time monitoring using `-i` command:

```bash
mbean -i 1000 java.lang:type=Threading *Count
```