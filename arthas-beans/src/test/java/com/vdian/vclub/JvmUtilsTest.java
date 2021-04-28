package com.vdian.vclub;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 以下测试的jvm参数均为：-Xms128m -Xmx128m
 */
public class JvmUtilsTest {

    /**
     * 在我的macbook上运行结果如下
     * allLoadedClasses->1050
     * com.vdian.vclub.JvmUtils@5bb21b69 com.vdian.vclub.JvmUtils@6b9651f3
     * before instances->[com.vdian.vclub.JvmUtils@5bb21b69, com.vdian.vclub.JvmUtils@6b9651f3]
     * size->16
     * count->2
     * sum size->32
     * null null
     * after instances->[]
     */
    @Test
    public void test01() {
        try {
            //调用native方法，获取已加载的类，不包括小类型(如int)
            Class<?>[] allLoadedClasses = JvmUtils.getAllLoadedClasses();
            System.out.println("allLoadedClasses->" + allLoadedClasses.length);

            //通过下面的例子，可以看到getInstances(Class<T> klass)拿到的是当前存活的所有对象
            WeakReference<JvmUtils> weakReference1 = new WeakReference<JvmUtils>(new JvmUtils());
            WeakReference<JvmUtils> weakReference2 = new WeakReference<JvmUtils>(new JvmUtils());
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            JvmUtils[] beforeInstances = JvmUtils.getInstances(JvmUtils.class);
            System.out.println("before instances->" + beforeInstances);
            System.out.println("size->" + JvmUtils.getInstanceSize(weakReference1.get()));
            System.out.println("count->" + JvmUtils.countInstances(JvmUtils.class));
            System.out.println("sum size->" + JvmUtils.sumInstanceSize(JvmUtils.class));
            beforeInstances = null;

            System.gc();
            Thread.sleep(100);
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            JvmUtils[] afterInstances = JvmUtils.getInstances(JvmUtils.class);
            System.out.println("after instances->" + afterInstances);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 没问题
     */
    @Test
    public void testGetInstancesMemoryLeak() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final AtomicLong totalTime = new AtomicLong();
        for (int i = 1; i <= 200000; i++) {
            long start = System.currentTimeMillis();
            WeakReference<Object[]> reference = new WeakReference<Object[]>(JvmUtils.getInstances(Object.class));
            Object[] instances = reference.get();
            long cost = System.currentTimeMillis() - start;
            totalTime.addAndGet(cost);
            System.out.println(i + " instance size:" + (instances == null ? 0 : instances.length) + ", cost " + cost + "ms avgCost " + totalTime.doubleValue() / i + "ms");
            instances = null;
            if (i % 100 == 0) {
                //fixme 如果注释掉下面这行代码，被动gc，仍然会挂掉，不知道为什么
                System.gc();
            }
        }
    }

    /**
     * 没问题
     */
    @Test
    public void testSumInstancesMemoryLeak() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final AtomicLong totalTime = new AtomicLong();
        for (int i = 1; i <= 200000; i++) {
            long start = System.currentTimeMillis();
            long sum = JvmUtils.sumInstanceSize(Object.class);
            long cost = System.currentTimeMillis() - start;
            totalTime.addAndGet(cost);
            System.out.println(i + " sum:" + sum + ", cost " + cost + "ms avgCost " + totalTime.doubleValue() / i + "ms");
        }
    }

    /**
     * 没问题
     */
    @Test
    public void testCountInstancesMemoryLeak() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final AtomicLong totalTime = new AtomicLong();
        for (int i = 1; i <= 200000; i++) {
            long start = System.currentTimeMillis();
            long count = JvmUtils.countInstances(Object.class);
            long cost = System.currentTimeMillis() - start;
            totalTime.addAndGet(cost);
            System.out.println(i + " count:" + count + ", cost " + cost + "ms avgCost " + totalTime.doubleValue() / i + "ms");
        }
    }

    /**
     * 没问题
     */
    @Test
    public void testGetAllLoadedClassesMemoryLeak() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final AtomicLong totalTime = new AtomicLong();
        for (int i = 1; i <= 200000; i++) {
            long start = System.currentTimeMillis();
            Class<?>[] allLoadedClasses = JvmUtils.getAllLoadedClasses();
            long cost = System.currentTimeMillis() - start;
            totalTime.addAndGet(cost);
            System.out.println(i + " class size:" + allLoadedClasses.length + ", cost " + cost + "ms avgCost " + totalTime.doubleValue() / i + "ms");
            allLoadedClasses = null;
        }
    }

}
