## A Tutorial for Beginners

### Prepare the demo

Save the following demo to `demo/Demo.java` to have the first try on Arthas. 

<sub>(If you are using Unix/Linux/Mac, then the commands below can be used.)</sub>

```bash
# you'd better find a proper location for the tutorial
$ mkdir demo && touch demo/Demo.java

# if there are indentation issues
# ":set paste" in vim command mode to enter insert & paste mode
$ vim demo/Demo.java 
# ":set nopaste" afterwards to reset
# ":wq" to save and exit
```

Copy and paste the following code to `demo/Demo.java`:

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
$ javac demo.Demo.java && java demo.Demo
```

### Start Arthas

#### Linux/Unix/Mac

```bash
./as.sh
```

- the user to run the *command* should have the same privilege as the owner of the target process, as a simple example you can try the following command if the target process is managed by user `admin`:

```bash
sudo su admin && ./as.sh
# Or
sudo -u admin -EH ./as.sh
```

- For more details of the booting script, please refer to [Start Arthas](start-arthas.md).
- If you cannot *attach* to the target process, please check the logs under `~/logs/arthas` for troubleshooting.
- Selecting the target process as:

```
$./as.sh 
Arthas script version: 3.0.4
Found existing java process, please choose one and hit RETURN.
* [1]: 1193 
  [2]: 1690 demo.Demo

# We select `2` here to check our Demo process and we then have
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

#### Windows

Open the *DOS* console, under the unzipped arthas folder execute `as.bat <pid>`


### dashboard
 [dashboard](dashboard.md)

```

```

### thread
 [thread](thread.md)

```

```

### jvm
 [jvm](jvm.md)

```

```

### sysprop
 [sysprop](sysprop.md)

```

```

### getstatic
 [getstatic](getstatic.md)

```

```

### sc
 [sc](sc.md)

```

```

### sm
 [sm](sm.md)

```

```

### dump
 [dump](dump.md)

```

```

### jad
 [jad](jad.md)

```

```

### classloader
 [classloader](classloader.md)

```

```

### redefine
 [redefine](redefine.md)

```

```

### monitor
 [monitor](monitor.md)

```

```

### watch
 [watch](watch.md)

```

```

### trace
 [trace](trace.md)

```

```

### stack
 [stack](stack.md)

```

```

### tt
 [tt](tt.md)

```

```

### options
 [options](options.md)

```

```

### Others

[more advanced usages](advanced-use.md)

### Exit Arthas

- `quit` or `exit` will just disconnect the current session (console connection) while Arthas still running;
- `shutdown` will terminate the Arthas completely stopping the Arthas server and all Arthas clients.

