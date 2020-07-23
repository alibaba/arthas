
```bash
$ jvm
RUNTIME
--------------------------------------------------------------------------------------------------------------
 MACHINE-NAME                   37@ff267334bb65
 JVM-START-TIME                 2020-07-23 07:50:36
 MANAGEMENT-SPEC-VERSION        1.2
 SPEC-NAME                      Java Virtual Machine Specification
 SPEC-VENDOR                    Oracle Corporation
 SPEC-VERSION                   1.8
 VM-NAME                        Java HotSpot(TM) 64-Bit Server VM
 VM-VENDOR                      Oracle Corporation
 VM-VERSION                     25.201-b09
 INPUT-ARGUMENTS                []
 CLASS-PATH                     demo-arthas-spring-boot.jar
 BOOT-CLASS-PATH                /usr/lib/jvm/java-8-oracle/jre/lib/resources.jar:/usr/lib/jvm/java-8-oracle/j
                                re/lib/rt.jar:/usr/lib/jvm/java-8-oracle/jre/lib/sunrsasign.jar:/usr/lib/jvm/
                                java-8-oracle/jre/lib/jsse.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jce.jar:/us
                                r/lib/jvm/java-8-oracle/jre/lib/charsets.jar:/usr/lib/jvm/java-8-oracle/jre/l
                                ib/jfr.jar:/usr/lib/jvm/java-8-oracle/jre/classes
 LIBRARY-PATH                   /usr/java/packages/lib/amd64:/usr/lib64:/lib64:/lib:/usr/lib

--------------------------------------------------------------------------------------------------------------
 CLASS-LOADING
--------------------------------------------------------------------------------------------------------------
 LOADED-CLASS-COUNT             7529
 TOTAL-LOADED-CLASS-COUNT       7529
 UNLOADED-CLASS-COUNT           0
 IS-VERBOSE                     false

--------------------------------------------------------------------------------------------------------------
 COMPILATION
--------------------------------------------------------------------------------------------------------------
 NAME                           HotSpot 64-Bit Tiered Compilers
 TOTAL-COMPILE-TIME             14921(ms)

--------------------------------------------------------------------------------------------------------------
 GARBAGE-COLLECTORS
--------------------------------------------------------------------------------------------------------------
 Copy                           152/523(ms)
 [count/time]
 MarkSweepCompact               3/268(ms)
 [count/time]

--------------------------------------------------------------------------------------------------------------
 MEMORY-MANAGERS
--------------------------------------------------------------------------------------------------------------
 CodeCacheManager               Code Cache

 Metaspace Manager              Metaspace
                                Compressed Class Space

 Copy                           Eden Space
                                Survivor Space

 MarkSweepCompact               Eden Space
                                Survivor Space
                                Tenured Gen


--------------------------------------------------------------------------------------------------------------
 MEMORY
--------------------------------------------------------------------------------------------------------------
 HEAP-MEMORY-USAGE              48005120(45.8 MiB)/12582912(12.0 MiB)/186515456(177.9 MiB)/37404168(35.7 MiB)
 [committed/init/max/used]
 NO-HEAP-MEMORY-USAGE           59834368(57.1 MiB)/2555904(2.4 MiB)/-1(-1 B)/58356376(55.7 MiB)
 [committed/init/max/used]
 PENDING-FINALIZE-COUNT         0

--------------------------------------------------------------------------------------------------------------
 OPERATING-SYSTEM
--------------------------------------------------------------------------------------------------------------
 OS                             Linux
 ARCH                           amd64
 PROCESSORS-COUNT               3
 LOAD-AVERAGE                   29.53
 VERSION                        4.15.0-52-generic

--------------------------------------------------------------------------------------------------------------
 THREAD
--------------------------------------------------------------------------------------------------------------
 COUNT                          30
 DAEMON-COUNT                   24
 PEAK-COUNT                     31
 STARTED-COUNT                  36
 DEADLOCK-COUNT                 0

--------------------------------------------------------------------------------------------------------------
 FILE-DESCRIPTOR
--------------------------------------------------------------------------------------------------------------
 MAX-FILE-DESCRIPTOR-COUNT      1048576
 OPEN-FILE-DESCRIPTOR-COUNT     100
Affect(row-cnt:0) cost in 88 ms.
```

