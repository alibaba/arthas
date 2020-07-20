
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
   vmoption PrintGCDetails
   vmoption PrintGCDetails true

 WIKI:
   https://alibaba.github.io/arthas/vmoption

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

`vmoption PrintGCDetails true`{{execute T2}}

```bash
[arthas@56963]$ vmoption PrintGCDetails true
Successfully updated the vm option.
PrintGCDetails=true
```
