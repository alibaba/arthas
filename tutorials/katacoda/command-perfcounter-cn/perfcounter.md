
查看当前JVM的 Perf Counter信息

`perfcounter -h`{{execute T2}}

```bash
 USAGE:
   perfcounter [-d] [-h]

 SUMMARY:
   Display the perf counter information.

 Examples:
   perfcounter
   perfcounter -d

 WIKI:
   https://arthas.aliyun.com/doc/perfcounter

 OPTIONS:
 -d, --details                        print all perf counter details
 -h, --help                           this help
```

## 使用参考

`perfcounter`{{execute T2}}

```bash
$ perfcounter
 java.ci.totalTime                            2325637411
 java.cls.loadedClasses                       3403
 java.cls.sharedLoadedClasses                 0
 java.cls.sharedUnloadedClasses               0
 java.cls.unloadedClasses                     0
 java.property.java.version                   11.0.4
 java.property.java.vm.info                   mixed mode
 java.property.java.vm.name                   OpenJDK 64-Bit Server VM
 ```

可以用-d参数打印更多信息：

`perfcounter -d`{{execute T2}}

```bash
$ perfcounter -d
 Name                                   Variability   Units        Value
---------------------------------------------------------------------------------
 java.ci.totalTime                      Monotonic     Ticks        3242526906
 java.cls.loadedClasses                 Monotonic     Events       3404
 java.cls.sharedLoadedClasses           Monotonic     Events       0
 java.cls.sharedUnloadedClasses         Monotonic     Events       0
 java.cls.unloadedClasses               Monotonic     Events       0
```

## 备注: 对于jdk9以上的应用

如果没有打印出信息，应用在启动时，加下面的参数：

`--add-opens java.base/jdk.internal.perf=ALL-UNNAMED --add-exports java.base/jdk.internal.perf=ALL-UNNAMED`
