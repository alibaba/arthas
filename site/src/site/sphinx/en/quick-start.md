Quick Start
===========

## 1. Start A Demo

Save the following demo to `demo/Demo.java` to start the entry tutorial:

<sub>(If you are using Unix/Linux/Mac, then the commands below can be used directly)</sub>

```bash
# you'd better find a proper location for the tutorial
$ mkdir demo && touch demo/Demo.java

# if there are indentation issues
# ":set paste" in vim command mode to enter insert & paste mode
$ vim demo/Demo.java 
# ":set nopaste" afterwards to reset
# ":wq" to save and exit
```

Copy and paste the following code:

```java
package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public class Demo {
    static class Counter {
        private static AtomicInteger count = new AtomicInteger(0);
        public synchronized static void increment() {
            count.incrementAndGet();
        }
        public synchronized static int value() {
            System.out.println("Processing...");
            return count.get();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 5; ++i) {
            new Thread(() -> {
                try {
                    while (true) {
                        Counter.increment();
                        System.out.println(Thread.currentThread().getName() + " counter: " + Counter.value());
                        testListAdd();
                        TimeUnit.SECONDS.sleep(5);
                    }
                } catch (InterruptedException ignored) {
                    ignored.printStackTrace();
                }
            }).start();
        }
    }

    private synchronized static void testListAdd() {
        System.out.println(Thread.currentThread().getName() + " running list add test");
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");

        List<String> list2 = new ArrayList<String>();
        list2.add("c");
        list2.add("d");

        int len = addTwoLists(list, list2);
    }

    private synchronized static <T> int addTwoLists(List<T> list1, List<T> list2) {
        list1.addAll(list2);
        return list1.size();
    }
}
```


Compile and start the demo
```bash
$ javac Demo.java && java Demo
```

## 2. Start Arthas

### Linux/Unix/Mac

```bash
./as.sh
```

1. the user to run the *command* should have the same privilege as the owner of the target process, as a simple example you can try the following command if the target process is managed by user `admin`:

    ```bash
    sudo su admin && ./as.sh
    # Or
    sudo -u admin -EH ./as.sh
    ```

2. For more details of the booting script, please refer to [Start Arthas](start-arthas.md).
3. If you cannot *attach* to the target process, please check the logs under `~/logs/arthas` for troubleshooting.

4. Selecting the target process as:

    ```
    $./as.sh 
    Arthas script version: 3.0.4
    Found existing java process, please choose one and hit RETURN.
    * [1]: 1193 
      [2]: 1690 demo.Demo
    ```

    We select `2` here to check our Demo process and we then have

    ```
    2
    Calculating attach execution time...
    Attaching to 1690 using version 3.0.5.20180922094548...

    real    0m1.385s
    user    0m0.305s
    sys 0m0.043s
    Attach success.
    Connecting to arthas server... current timestamp is 1538193225
    Trying 127.0.0.1...
    Connected to localhost.
    Escape character is '^]'.
      ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.                           
     /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'                          
    |  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.                          
    |  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |                         
    `--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'                          
                                                                                    

    wiki: https://alibaba.github.io/arthas
    version: 3.0.5.20180922094548
    pid: 1690
    timestamp: 1538193226393

    $ 
    ```

### Windows

Open the *DOS* console, under the unzipped arthas folder execute `as.bat <pid>`


## 3. Check the Dashboard

Type in [dashboard](dashboard.md) and hit the *ENTER*, you will see it as (`Ctrl+C` to stop)

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

## 4. watch

Use [watch](watch.md) to check the returned value of `Counter.value()`:

```
$ watch demo.Demo$Counter value returnObj
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 54 ms.
ts=2018-09-29 12:02:05;result=@Integer[42]
ts=2018-09-29 12:02:05;result=@Integer[43]
ts=2018-09-29 12:02:05;result=@Integer[43]
ts=2018-09-29 12:02:05;result=@Integer[44]
ts=2018-09-29 12:02:05;result=@Integer[45]
ts=2018-09-29 12:02:10;result=@Integer[46]
ts=2018-09-29 12:02:10;result=@Integer[47]
ts=2018-09-29 12:02:10;result=@Integer[49]
ts=2018-09-29 12:02:10;result=@Integer[49]
ts=2018-09-29 12:02:10;result=@Integer[50]
```

[more advanced usages](advanced-use.md)

## 5. Exit Arthas

- `quit` or `exit` will just disconnect the current session (console connection) while Arthas still running;
- `shutdown` will terminate the Arthas completely stopping the Arthas server and all Arthas clients.
