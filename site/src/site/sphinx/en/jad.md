jad
===

De-compile specified loaded classes.

`jad` helps to *de-compile* the Java bytecode to the source code assisting you to better understand the logic behind.

F.Y.I
* the de-compiled code will be grammatically highlighted for readability in Arthas console;
* there might be some trivial grammar errors but it won't affect the logic understanding.

### Options

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|[c:]|hashcode of the class loader that loaded the class|
|[E]|turn on regex matching while the default is wildcard matching|

### Usage

When several class loaders loaded the same class:
1. `jad` to get the hashcode of the class loader;
2. `jad -c <hashcode>` to get the de-compiled class loaded by the specified class loader.

```java
$ jad demo.Demo

ClassLoader:                                                                                                                                                                         
+-sun.misc.Launcher$AppClassLoader@659e0bfd                                                                                                                                          
  +-sun.misc.Launcher$ExtClassLoader@758c1b43                                                                                                                                        

Location:                                                                                                                                                                            
/Users/hello/test/                                                                                                                                                                 

/*
 * Decompiled with CFR 0_132.
 */
package demo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Demo {
    public static void main(String[] arrstring) throws InterruptedException {
        for (int i = 0; i < 5; ++i) {
            new Thread(() -> {
                    try {
                    do {
                        Counter.increment();
                        System.out.println(Thread.currentThread().getName() + " counter: " + Counter.value());
                        Demo.testListAdd();
                        TimeUnit.SECONDS.sleep(5L);
                    
                    } while (true);
                
                    }
                    catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                    return;
                    }
                    }).start();
        }
    }

    private static synchronized <T> int addTwoLists(List<T> list, List<T> list2) {
        list.addAll(list2);
        return list.size();
    }

    private static synchronized void testListAdd() {
        System.out.println(Thread.currentThread().getName() + " running list add test");
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("a");
        arrayList.add("b");
        ArrayList<String> arrayList2 = new ArrayList<String>();
        arrayList2.add("c");
        arrayList2.add("d");
        int n = Demo.addTwoLists(arrayList, arrayList2);
    }
    static class Counter {
        private static AtomicInteger count = new AtomicInteger(0);
        Counter() {
        }
        public static synchronized int value() {
            System.out.println("Processing...");
            return count.get();
        }
        public static synchronized void increment() {
            count.incrementAndGet();
        }
    }
}

Affect(row-cnt:2) cost in 772 ms.
```

F.Y.I

Inner class is not yet supported, you can just check the *outer* class to further check the inner class. 
