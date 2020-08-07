vmoption
===

[`vmoption`在线教程](https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-vmoption)

> 查看，更新VM诊断相关的参数

### 使用参考

#### 查看所有的option

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

#### 查看指定的option

```bash
[arthas@56963]$ vmoption PrintGCDetails
 KEY                    VALUE                   ORIGIN                 WRITEABLE
---------------------------------------------------------------------------------------------
 PrintGCDetails         false                   MANAGEMENT             true
```

#### 更新指定的option

```bash
[arthas@56963]$ vmoption PrintGCDetails true
Successfully updated the vm option.
PrintGCDetails=true
```
