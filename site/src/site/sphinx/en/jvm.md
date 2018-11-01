jvm
===

> Check the current JVM's info

### Usage

```
$ jvm
 CATEGORY            INFO
------------------------------------------------------------------------------------------------------------------------------------

 RUNTIME              MACHINE-NAME             hellodeMacBook-Air.local
                      JVM-START-TIME           2015-12-23 10:54:18
                      MANAGEMENT-SPEC-VERSION  1.2
                      SPEC-NAME                Java Virtual Machine Specification
                      SPEC-VENDOR              Oracle Corporation
                      SPEC-VERSION             1.8
                      VM-NAME                  Java HotSpot(TM) 64-Bit Server VM
                      VM-VENDOR                Oracle Corporation
                      VM-VERSION               25.60-b23
                      INPUT-ARGUMENTS          -Xbootclasspath/a:/Users/hello/.jenv/versions/1.8/lib/tools.jar
                                               -Djava.util.logging.config.file=/Users/hello/code/java/crash/packaging/target/
                                               conf/logging.properties

                      CLASS-PATH               /Users/hello/.jenv/versions/1.8/lib/tools.jar:/Users/hello/code/java/cras
                                               h/packaging/target/bin/crash.cli-1.3.2-SNAPSHOT.jar::/Users/hello/code/java/cr
                                               ash/packaging/target/lib/bcpkix-jdk15on-1.51.jar:/Users/hello/code/java/crash/
                                               packaging/target/lib/bcprov-jdk15on-1.51.jar:/Users/hello/code/java/crash/pack
                                               aging/target/lib/crash.connectors.ssh-1.3.2-SNAPSHOT-standalone.jar:/Users/hengyuna
                                               bc/code/java/crash/packaging/target/lib/crash.connectors.telnet-1.3.2-SNAPSHOT-stan
                                               dalone.jar:/Users/hello/code/java/crash/packaging/target/lib/crash.shell-1.3.2
                                               -SNAPSHOT.jar:/Users/hello/code/java/crash/packaging/target/lib/groovy-all-1.8
                                               .9.jar:/Users/hello/code/java/crash/packaging/target/lib/ivy-2.2.0.jar
                      BOOT-CLASS-PATH          /Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/resources.j
                                               ar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar:/
                                               Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/sunrsasign.j
                                               ar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/jsse.jar
                                               :/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/jce.jar:/L
                                               ibrary/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/charsets.jar:
                                               /Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/jfr.jar:/Li
                                               brary/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/classes:/Users/hen
                                               gyunabc/.jenv/versions/1.8/lib/tools.jar
                      LIBRARY-PATH             /Users/hello/Library/Java/Extensions:/Library/Java/Extensions:/Network/Library
                                               /Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java:.

 CLASS-LOADING        LOADED-CLASS-COUNT        4264
                      TOTAL-LOADED-CLASS-COUNT  4264
                      UNLOADED-CLASS-COUNT      0
                      IS-VERBOSE                false

 COMPILATION          NAME                HotSpot 64-Bit Tiered Compilers
                      TOTAL-COMPILE-TIME  5145(ms)

 GARBAGE-COLLECTORS   PS Scavenge   6/74(ms)
                      [count/time]
                      PS MarkSweep  1/64(ms)
                      [count/time]

 MEMORY-MANAGERS      CodeCacheManager   Code Cache

                      Metaspace Manager  Metaspace
                                         Compressed Class Space

                      PS Scavenge        PS Eden Space
                                         PS Survivor Space

                      PS MarkSweep       PS Eden Space
                                         PS Survivor Space
                                         PS Old Gen


 MEMORY               HEAP-MEMORY-USAGE          186646528/134217728/1908932608/31245568
                      [committed/init/max/used]
                      NO-HEAP-MEMORY-USAGE       35520512/2555904/-1/34584616
                      [committed/init/max/used]
                      PENDING-FINALIZE-COUNT     0

 OPERATING-SYSTEM     OS                Mac OS X
                      ARCH              x86_64
                      PROCESSORS-COUNT  4
                      LOAD-AVERAGE      2.328125
                      VERSION           10.10.5

 THREAD               COUNT          16
                      DAEMON-COUNT   10
                      PEAK-COUNT     18
                      STARTED-COUNT  19
                      DEADLOCK-COUNT  0
                      
 FILE-DESCRIPTOR
 
                      MAX-FILE-DESCRIPTOR-COUNT  10240
                      OPEN-FILE-DESCRIPTOR-COUNT 648
Affect cost in 2 ms.
```

### Thread related

* COUNT: the count of active threads 
* DAEMON-COUNT: the count of active daemon threads
* PEAK-COUNT: the maximum count of the live threads since JVM starts
* STARTED-COUNT: the total count of the created threads since JVM starts
* DEADLOCK-COUNT: the count of deadlocked threads 


### File descriptor related

* MAX-FILE-DESCRIPTOR-COUNT：the count of max file descriptor JVM process can open
* OPEN-FILE-DESCRIPTOR-COUNT：the current count of file descriptor JVM process open
