
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

`vmoption PrintGCDetails true`{{execute T2}}

```bash
[arthas@56963]$ vmoption PrintGCDetails true
Successfully updated the vm option.
PrintGCDetails=true
```

Now，if we switch to the `Terminal` where arthas demo is running，and use `Ctrl+c` to exit，you will find it prints out Garbage Collection details：

```bash
Heap
 def new generation   total 10432K, used 5682K [0x00000000f4800000, 0x00000000f5350000, 0x00000000f8550000)
  eden space 9280K,  61% used [0x00000000f4800000, 0x00000000f4d8cad0, 0x00000000f5110000)
  from space 1152K,   0% used [0x00000000f5110000, 0x00000000f5110000, 0x00000000f5230000)
  to   space 1152K,   0% used [0x00000000f5230000, 0x00000000f5230000, 0x00000000f5350000)
 tenured generation   total 22992K, used 13795K [0x00000000f8550000, 0x00000000f9bc4000, 0x0000000100000000)
   the space 22992K,  59% used [0x00000000f8550000, 0x00000000f92c8cc8, 0x00000000f92c8e00, 0x00000000f9bc4000)
 Metaspace       used 14926K, capacity 15128K, committed 15360K, reserved 1062912K
  class space    used 1895K, capacity 1954K, committed 2048K, reserved 1048576K
```
