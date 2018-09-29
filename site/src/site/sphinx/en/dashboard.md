dashboard
=========

This is the realtime dashboard for the system; press `Ctrl+C` to exit.

When running in *Ali-tomcat*, the dashboard will present the realtime statistics of the tomcat including [QPS](https://en.wikipedia.org/wiki/Queries_per_second), RT, error counts, thread profile and the like.

### A Demo

```
$ dashboard
ID             NAME                                         GROUP                          PRIORITY       STATE          %CPU           TIME           INTERRUPTED    DAEMON         
27             Timer-for-arthas-dashboard-17515e8b-040f-449 system                         10             RUNNABLE       58             0:0            false          true           
21             nioEventLoopGroup-3-1                        system                         10             RUNNABLE       41             0:0            false          false          
17             AsyncAppender-Worker-arthas-cache.result.Asy system                         9              WAITING        0              0:0            false          true           
15             Attach Listener                              system                         9              RUNNABLE       0              0:0            false          true           
14             DestroyJavaVM                                main                           5              RUNNABLE       0              0:0            false          false          
3              Finalizer                                    system                         8              WAITING        0              0:0            false          true           
2              Reference Handler                            system                         10             WAITING        0              0:0            false          true           
4              Signal Dispatcher                            system                         9              RUNNABLE       0              0:0            false          true           
9              Thread-0                                     main                           5              TIMED_WAITING  0              0:0            false          false          
10             Thread-1                                     main                           5              TIMED_WAITING  0              0:0            false          false          
11             Thread-2                                     main                           5              TIMED_WAITING  0              0:0            false          false          
12             Thread-3                                     main                           5              TIMED_WAITING  0              0:0            false          false          
13             Thread-4                                     main                           5              TIMED_WAITING  0              0:0            false          false          
25             as-command-execute-daemon                    system                         10             TIMED_WAITING  0              0:0            false          true           
19             job-timeout                                  system                         9              TIMED_WAITING  0              0:0            false          true           
20             nioEventLoopGroup-2-1                        system                         10             RUNNABLE       0              0:0            false          false          
24             nioEventLoopGroup-2-2                        system                         10             RUNNABLE       0              0:0            false          false          
22             pool-1-thread-1                              system                         5              TIMED_WAITING  0              0:0            false          false          
23             pool-2-thread-1                              system                         5              WAITING        0              0:0            false          false          
                                                                                                                                                                                     
                                                                                                                                                                                     
                                                                                                                                                                                     
Memory                                 used         total        max          usage        GC                                                                                        
heap                                   34M          155M         1820M        1.91%        gc.ps_scavenge.count                         4                                            
ps_eden_space                          19M          65M          672M         2.86%        gc.ps_scavenge.time(ms)                      44                                           
ps_survivor_space                      4M           5M           5M           99.69%       gc.ps_marksweep.count                        0                                            
ps_old_gen                             10M          85M          1365M        0.78%        gc.ps_marksweep.time(ms)                     0                                            
nonheap                                20M          20M          -1           97.71%                                                                                                 
code_cache                             5M           5M           240M         2.12%                                                                                                  
metaspace                              13M          13M          -1           97.67%                                                                                                 
compressed_class_space                 1M           1M           1024M        0.16%                                                                                                  
direct                                 0K           0K           -            Infinity%                                                                                              
mapped                                 0K           0K           -            NaN%                                                                                                   
                                                                                                                                                                                     
Runtime                                                                                                                                                                              
os.name                                       Mac OS X                                                                                                                               
os.version                                    10.11.6                                                                                                                                
java.version                                  1.8.0_73                                                                                                                               
java.home                                     /Library/Java/JavaVirtualMachines/jdk1.8.0_7                                                                                           
                                              3.jdk/Contents/Home/jre                                                                                                                
systemload.average                            1.29                                                                                                                                   
processors                                    4         
```

### Specification

* ID: thread ID in JVM (this is different from the nativeID in thread dump)
* NAME: thread name
* GROUP: group the thread is in
* STATE: the current state of the thread
* PRIORITY: within 1 ~ 10 (the higher the number, the higher the priority)
* CPU%: CPU usage ratio within 100ms
* TIME: total running time in minute:second format
* INTERRUPTED: the thread interrupted state
* DAEMON: is daemon thread or not

### Screenshots

![alt text](../_static/dashboard.png "dashboard")
