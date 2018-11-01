Quick Start
===========

## 1. Start Demo Application

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

Save the contents above into `Demo.java`, then compile it under the command line:

```bash
javac Demo.java
java Demo
```

You can also save and start the code in IDE.

## 2. Start Arthas

### Linux/Unix/Mac

Execute the following command in the command line:

```bash
./as.sh
```

> The user to run this script *MUST* have the same privilege as the owner of the target process, as a simple example you can try the following command if the target process is managed by user `admin`: `sudo su admin && ./as.sh` or `sudo -u admin -EH ./as.sh`. For more details on the bootstrap script, please refer to [Start Arthas](start-arthas.md). If you cannot be able to attach to the target process, please check the logs under `~/logs/arthas` for troubleshooting.

Select the target Java process to attach:

```bash
$ ./as.sh
Arthas script version: 3.0.2
Found existing java process, please choose one and hit RETURN.
* [1]: 95428 
  [2]: 22647 org.jetbrains.jps.cmdline.Launcher
  [3]: 21736
  [4]: 13560 Demo
```

The 'Demo' process is the fourth as shown above, press '4' then 'Enter'. Arthas will attach to the target process, and start to output:

```bash
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
```

### Windows

Open 'Command' window, execute `as.bat <pid>` from where the Arthas package file is unzipped.


## 3. Check the Dashboard

Type '[dashboard](dashboard.md)' and hit 'ENTER', a brief report on the current process will be shown as below, pls. `Ctrl+C` to stop:

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

Use '[watch](watch.md)' to check the returned value of `Counter.value()`:

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

Pls. refer to [advanced usages](advanced-use.md) for more information.

## 5. Exit Arthas

Use `quit` or `exit` to disconnect from the current process. The Arthas instance attached to the target process continues to live inside the process, and its port is standby for further connection.

Use `shutdown` to have Arthas completely quit from the target process.
