Quick Start
===========

## 1. Start A Demo

Save the following code to a `Demo.java` and run the commands in shell as 

```bash
javac Demo.java && java Demo
```

```java
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public class Demo {
    static class Counter {
        private static AtomicInteger count = new AtomicInteger(0);
        public static void increment() {
            count.incrementAndGet();
        
        }
        public static int value() {
            return count.get();
        
        }
    
    }

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            Counter.increment();
            System.out.println("counter: " + Counter.value());
            TimeUnit.SECONDS.sleep(1);
        
        }
    
    }

}
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
3. If you cannot *attach* the target process, please check the logs under `~/logs/arthas` for troubleshooting.

4. Selecting the target process as:

    ```
    $ ./as.sh
    Arthas script version: 3.0.2
    Found existing java process, please choose one and hit RETURN.
    * [1]: 95428 
      [2]: 22647 org.jetbrains.jps.cmdline.Launcher
      [3]: 21736
      [4]: 13560 Demo
    ```

    We select `4` to check our Demo process and we then have

    ```
    Connecting to arthas server... current timestamp is 1536656867
    Trying 127.0.0.1...
    Connected to 127.0.0.1.
    Escape character is '^]'.
      ,---.  ,------. ,--------.,--.  ,--.  ,---.   ,---.
      /  O  \ |  .--. ''--.  .--'|  '--'  | /  O  \ '   .-'
    |  .-.  ||  '--'.'   |  |   |  .--.  ||  .-.  |`.  `-.
    |  | |  ||  |\  \    |  |   |  |  |  ||  | |  |.-'    |
    `--' `--'`--' '--'   `--'   `--'  `--'`--' `--'`-----'


    wiki: https://alibaba.github.io/arthas
    version: 3.0.1-RC-SNAPSHOT
    pid: 13560
    timestamp: 1536656867894
    
    $ 
    ```

### Windows

Open the *DOS* console, under the unzipped arthas folder execture `as.bat <pid>`


## 3. Check the Dashboard

Type in [dashboard](dashboard.md) and hit the *ENTER*, you will see it as (`Ctrl+C` to stop)

```
$ dashboard
ID     NAME                   GROUP          PRIORI STATE  %CPU    TIME   INTERRU DAEMON
17     pool-2-thread-1        system         5      WAITIN 67      0:0    false   false
27     Timer-for-arthas-dashb system         10     RUNNAB 32      0:0    false   true
11     AsyncAppender-Worker-a system         9      WAITIN 0       0:0    false   true
9      Attach Listener        system         9      RUNNAB 0       0:0    false   true
3      Finalizer              system         8      WAITIN 0       0:0    false   true
2      Reference Handler      system         10     WAITIN 0       0:0    false   true
4      Signal Dispatcher      system         9      RUNNAB 0       0:0    false   true
26     as-command-execute-dae system         10     TIMED_ 0       0:0    false   true
13     job-timeout            system         9      TIMED_ 0       0:0    false   true
1      main                   main           5      TIMED_ 0       0:0    false   false
14     nioEventLoopGroup-2-1  system         10     RUNNAB 0       0:0    false   false
18     nioEventLoopGroup-2-2  system         10     RUNNAB 0       0:0    false   false
23     nioEventLoopGroup-2-3  system         10     RUNNAB 0       0:0    false   false
15     nioEventLoopGroup-3-1  system         10     RUNNAB 0       0:0    false   false
Memory             used   total max    usage GC
heap               32M    155M  1820M  1.77% gc.ps_scavenge.count  4
ps_eden_space      14M    65M   672M   2.21% gc.ps_scavenge.time(m 166
ps_survivor_space  4M     5M    5M           s)
ps_old_gen         12M    85M   1365M  0.91% gc.ps_marksweep.count 0
nonheap            20M    23M   -1           gc.ps_marksweep.time( 0
code_cache         3M     5M    240M   1.32% ms )
Runtime
os.name                Mac OS X
os.version             10.13.4
java.version           1.8.0_162
java.home              /Library/Java/JavaVir
                       tualMachines/jdk1.8.0
                       _162.jdk/Contents/Hom
                       e/jre
```

## 4. watch

Input [watch](watch.md) to check the returned value of `Counter.value()`:

```
$ watch Demo$Counter value returnObj
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 29 ms.
ts=2018-09-10 17:53:11;result=@Integer[621]
ts=2018-09-10 17:53:12;result=@Integer[622]
ts=2018-09-10 17:53:13;result=@Integer[623]
ts=2018-09-10 17:53:14;result=@Integer[624]
ts=2018-09-10 17:53:15;result=@Integer[625]
```

[more advanced usages](advanced-use.md)

## 5. Exit Arthas

- `quit` or `exit` will just disconnect the current console connection while Arthas still running in the target process
- `shutdown` will terminate the Arthas completely
