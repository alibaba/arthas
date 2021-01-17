package com.vdian.vclub;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class MemoryAnalyzerTest {

    /**
     * 在我的macbook上运行结果如下
     * allLoadedClasses->951
     * com.vdian.vclub.MemoryAnalyzer@7d907bac com.vdian.vclub.MemoryAnalyzer@7791a895
     * before instances->[com.vdian.vclub.MemoryAnalyzer@7d907bac, com.vdian.vclub.MemoryAnalyzer@7791a895]
     * size->16
     * count->67174
     * sum size->15237536
     * null null
     * after instances->[]
     */
    @Test
    public void test01() {
        try {
            //调用native方法，获取已加载的类，不包括小类型(如int)
            LinkedList<Class<?>> allLoadedClasses = MemoryAnalyzer.getAllLoadedClasses();
            System.out.println("allLoadedClasses->" + allLoadedClasses.size());

            //通过下面的例子，可以看到getInstances(Class<T> klass)拿到的是当前存活的所有对象
            WeakReference<MemoryAnalyzer> weakReference1 = new WeakReference<>(new MemoryAnalyzer());
            WeakReference<MemoryAnalyzer> weakReference2 = new WeakReference<>(new MemoryAnalyzer());
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            LinkedList<MemoryAnalyzer> beforeInstances = MemoryAnalyzer.getInstances(MemoryAnalyzer.class);
            System.out.println("before instances->" + beforeInstances);
            System.out.println("size->" + MemoryAnalyzer.getInstanceSize(weakReference1.get()));
            System.out.println("count->" + MemoryAnalyzer.countInstances(Object.class));
            System.out.println("sum size->" + MemoryAnalyzer.sumInstanceSize(Object.class));
            beforeInstances = null;

            System.gc();
            Thread.sleep(100);
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            LinkedList<MemoryAnalyzer> afterInstances = MemoryAnalyzer.getInstances(MemoryAnalyzer.class);
            System.out.println("after instances->" + afterInstances);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 在我的macbook上运行结果如下
     * {class java.lang.Object, sumSize=4345.08 KB, percentage=48.0%}
     * {class [I, sumSize=2200.88 KB, percentage=25.0%}
     * {class [B, sumSize=811.25 KB, percentage=9.0%}
     * {class java.lang.Class, sumSize=564.77 KB, percentage=6.0%}
     * {class [C, sumSize=278.63 KB, percentage=3.0%}
     * {class [Ljava.lang.Object;, sumSize=86.17 KB, percentage=1.0%}
     * {class java.lang.String, sumSize=83.3 KB, percentage=1.0%}
     * {class java.util.LinkedList$Node, sumSize=50.77 KB, percentage=1.0%}
     * cost time->656ms
     */
    @Test
    public void test02() {
        long start = System.currentTimeMillis();
        List<MemoryInfo> list = MemoryAnalyzer.analyze(false);
        long end = System.currentTimeMillis();
        for (MemoryInfo info : list) {
            System.out.println(info);
        }
        System.out.println("cost time->" + (end - start) + "ms");
    }

    @Test
    public void test03() {
        long start = System.currentTimeMillis();
        List<MemoryInfo> list = MemoryAnalyzer.analyze(true);
        long end = System.currentTimeMillis();
        for (MemoryInfo info : list) {
            System.out.println(info);
        }
        System.out.println("cost time->" + (end - start) + "ms");
    }
}
