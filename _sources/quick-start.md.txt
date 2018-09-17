快速入门
===

## 1. 启动Demo

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

把上面的内容保存到`Demo.java`里，然后在命令行下编绎启动：
```bash
javac Demo.java
java Demo
```

也可以把代码保存到IDE里，然后启动。

## 2. 启动arthas

### Linux/Unix/Mac
在命令行下面执行：

```bash
./as.sh
```

> 执行该脚本的用户需要和目标进程具有相同的权限。比如以`admin`用户来执行：
> `sudo su admin && ./as.sh` 或 `sudo -u admin -EH ./as.sh`。
> 详细的启动脚本说明，请参考[这里](start-arthas.md)。
> 如果attatch不上目标进程，可以查看`~/logs/arthas/` 目录下的日志。

选择应用java进程：

```
$ ./as.sh
Arthas script version: 3.0.2
Found existing java process, please choose one and hit RETURN.
* [1]: 95428 
  [2]: 22647 org.jetbrains.jps.cmdline.Launcher
  [3]: 21736
  [4]: 13560 Demo
```

Demo进程是第4个，则输入4，再输入回车/enter。Arthas会attach到目标进程上，并输出日志：

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
```

### Windows

打开Dos命令行窗口，在解压的arthas目录下执行`as.bat`。


## 3. 查看dashboard

输入[dashboard](dashboard.md)，按enter/回车，会展示当前进程的信息，按`ctrl+c`可以中断执行。

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
code_cache         3M     5M    240M   1.32% ms)
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

通过[watch](watch.md)命令来查看`Counter.value()`函数的返回值：

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

更多的功能可以查看[进阶使用](advanced-use.md)。

## 退出arthas

如果只是退出当前的连接，可以用`quit`或者`exit`命令。Attach到目标进程上的arthas还会继续运行，端口会保持开放，下次连接时可以直接连接上。

如果想完全退出arthas，可以执行`shutdown`命令。