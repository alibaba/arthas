
查看，更新VM诊断相关的参数

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

## 使用参考

### 查看所有的option：

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

### 查看指定的option

`vmoption PrintGC`{{execute T2}}

```bash
$ vmoption PrintGC
 KEY                 VALUE                ORIGIN              WRITEABLE
---------------------------------------------------------------------------------
 PrintGC             false                MANAGEMENT          true
```

### 更新指定的option

`vmoption PrintGC true`{{execute T2}}

```bash
$ vmoption PrintGC true
Successfully updated the vm option.
 NAME     BEFORE-VALUE  AFTER-VALUE
------------------------------------
 PrintGC  false         true
```

再使用`vmtool`命令执行强制GC，则可以在`Terminal 1`看到打印出GC日志：

`vmtool --action forceGc`{{execute T2}}

```
[GC (JvmtiEnv ForceGarbageCollection)  33752K->19564K(251392K), 0.0082960 secs]
[Full GC (JvmtiEnv ForceGarbageCollection)  19564K->17091K(166912K), 0.0271085 secs]
```

### 配置打印GC详情

`vmoption PrintGCDetails true`{{execute T2}}

```bash
$ vmoption PrintGCDetails true
Successfully updated the vm option.
 NAME            BEFORE-VALUE  AFTER-VALUE
-------------------------------------------
 PrintGCDetails  false         true
```

再使用`vmtool`命令执行强制GC，则可以在`Terminal 1`看到打印出GC详情：

`vmtool --action forceGc`{{execute T2}}

```
[GC (JvmtiEnv ForceGarbageCollection) [PSYoungGen: 4395K->352K(76288K)] 21487K->17443K(166912K), 0.0013122 secs] [Times: user=0.01 sys=0.00, real=0.00 secs]
[Full GC (JvmtiEnv ForceGarbageCollection) [PSYoungGen: 352K->0K(76288K)] [ParOldGen: 17091K->16076K(88064K)] 17443K->16076K(164352K), [Metaspace: 20651K->20651K(1069056K)], 0.0251548 secs] [Times: user=0.15 sys=0.01, real=0.02 secs]
```