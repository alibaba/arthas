
Display, and update the vm diagnostic options.

`vmoption -h`{{execute T2}}

```bash
[arthas@48]$ vmoption -h
 USAGE:
   vmoption [-h] [name] [value]

 SUMMARY:
   Display, and update the vm diagnostic options.

 Examples:
   vmoption
   vmoption PrintGC
   vmoption PrintGC true

 WIKI:
   https://arthas.aliyun.com/doc/vmoption

 OPTIONS:
 -h, --help                           this help
 <name>                               VMOption name
 <value>                              VMOption value
 ```

## Usage

### View all options

`vmoption`{{execute T2}}

```bash
[arthas@56963]$ vmoption
 KEY                    VALUE                   ORIGIN                 WRITEABLE
---------------------------------------------------------------------------------------------
 HeapDumpBeforeFullGC   false                   DEFAULT                true
 HeapDumpAfterFullGC    false                   DEFAULT                true
 HeapDumpOnOutOfMemory  false                   DEFAULT                true
 Error
 HeapDumpPath                                   DEFAULT                true
 CMSAbortablePrecleanW  100                     DEFAULT                true
 aitMillis
 CMSWaitDuration        2000                    DEFAULT                true
 CMSTriggerInterval     -1                      DEFAULT                true
 PrintGC                false                   DEFAULT                true
 PrintGCDetails         true                    MANAGEMENT             true
 PrintGCDateStamps      false                   DEFAULT                true
 PrintGCTimeStamps      false                   DEFAULT                true
 PrintGCID              false                   DEFAULT                true
 PrintClassHistogramBe  false                   DEFAULT                true
 foreFullGC
 PrintClassHistogramAf  false                   DEFAULT                true
 terFullGC
 PrintClassHistogram    false                   DEFAULT                true
 MinHeapFreeRatio       0                       DEFAULT                true
 MaxHeapFreeRatio       100                     DEFAULT                true
 PrintConcurrentLocks   false                   DEFAULT                true
```

### View individual option

`vmoption PrintGCDetails`{{execute T2}}

```bash
[arthas@56963]$ vmoption PrintGCDetails
 KEY                    VALUE                   ORIGIN                 WRITEABLE
---------------------------------------------------------------------------------------------
 PrintGCDetails         false                   MANAGEMENT             true
```

### Update individual option
`vmoption PrintGC true`{{execute T2}}

```bash
$ vmoption PrintGC true
Successfully updated the vm option.
 NAME     BEFORE-VALUE  AFTER-VALUE
------------------------------------
 PrintGC  false         true
```

Then use the `vmtool` command to force GC, you can see the GC log printed in `Terminal 1`:

`vmtool --action forceGc`{{execute T2}}

```
[GC (JvmtiEnv ForceGarbageCollection)  33752K->19564K(251392K), 0.0082960 secs]
[Full GC (JvmtiEnv ForceGarbageCollection)  19564K->17091K(166912K), 0.0271085 secs]
```

### Configure print GC details

`vmoption PrintGCDetails true`{{execute T2}}

```bash
$ vmoption PrintGCDetails true
Successfully updated the vm option.
 NAME            BEFORE-VALUE  AFTER-VALUE
-------------------------------------------
 PrintGCDetails  false         true
```

Then use the `vmtool` command to force GC, you can see the GC details printed in `Terminal 1`:

`vmtool --action forceGc`{{execute T2}}

```
[GC (JvmtiEnv ForceGarbageCollection) [PSYoungGen: 4395K->352K(76288K)] 21487K->17443K(166912K), 0.0013122 secs] [Times: user=0.01 sys=0.00, real=0.00 secs]
[Full GC (JvmtiEnv ForceGarbageCollection) [PSYoungGen: 352K->0K(76288K)] [ParOldGen: 17091K->16076K(88064K)] 17443K->16076K(164352K), [Metaspace: 20651K->20651K(1069056K)], 0.0251548 secs] [Times: user=0.15 sys=0.01, real=0.02 secs]
```