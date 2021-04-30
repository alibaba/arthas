package arthas;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.arthas.common.VmToolUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 以下本地测试的jvm参数均为：-Xms128m -Xmx128m
 */
public class VmToolTest {

    //本地测试请改成5000来跑
    private static final int ROUND = 5;

    /**
     * macbook上运行结果如下
     * allLoadedClasses->1050
     * arthas.VmTool@5bb21b69 arthas.VmTool@6b9651f3
     * before instances->[arthas.VmTool@5bb21b69, arthas.VmTool@6b9651f3]
     * size->16
     * count->2
     * sum size->32
     * null null
     * after instances->[]
     */
    @Test
    public void testIsSnapshot() {
        try {
            VmTool vmtool = initVmTool();
            //调用native方法，获取已加载的类，不包括小类型(如int)
            Class<?>[] allLoadedClasses = vmtool.getAllLoadedClasses();
            System.out.println("allLoadedClasses->" + allLoadedClasses.length);

            //通过下面的例子，可以看到getInstances(Class<T> klass)拿到的是当前存活的所有对象
            WeakReference<VmToolTest> weakReference1 = new WeakReference<VmToolTest>(new VmToolTest());
            WeakReference<VmToolTest> weakReference2 = new WeakReference<VmToolTest>(new VmToolTest());
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            VmTool[] beforeInstances = vmtool.getInstances(VmTool.class);
            System.out.println("before instances->" + Arrays.toString(beforeInstances));
            System.out.println("size->" + vmtool.getInstanceSize(weakReference1.get()));
            System.out.println("count->" + vmtool.countInstances(VmTool.class));
            System.out.println("sum size->" + vmtool.sumInstanceSize(VmTool.class));
            beforeInstances = null;

            vmtool.forceGc();
            Thread.sleep(100);
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            VmTool[] afterInstances = vmtool.getInstances(VmTool.class);
            System.out.println("after instances->" + Arrays.toString(afterInstances));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VmTool initVmTool() {
        String path = VmTool.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        System.err.println(path);

        String libPath = new File(path, VmToolUtils.detectLibName()).getAbsolutePath();
        return VmTool.getInstance(libPath);
    }

    @Test
    public void testGetInstancesMemoryLeak() {
        //这里睡20s是为了方便用jprofiler连接上进程
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        VmTool vmtool = initVmTool();
        final AtomicLong totalTime = new AtomicLong();
        //本地测试请改成200000
        for (int i = 1; i <= 2; i++) {
            long start = System.currentTimeMillis();
            WeakReference<Object[]> reference = new WeakReference<Object[]>(vmtool.getInstances(Object.class));
            Object[] instances = reference.get();
            long cost = System.currentTimeMillis() - start;
            totalTime.addAndGet(cost);
            System.out.println(i + " instance size:" + (instances == null ? 0 : instances.length) + ", cost " + cost + "ms avgCost " + totalTime.doubleValue() / i + "ms");
            instances = null;
            vmtool.forceGc();
        }
    }

    @Test
    public void testSumInstancesMemoryLeak() {
        //这里睡20s是为了方便用jprofiler连接上进程
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        VmTool vmtool = initVmTool();
        final AtomicLong totalTime = new AtomicLong();
        //本地测试请改成200000
        for (int i = 1; i <= 2; i++) {
            long start = System.currentTimeMillis();
            long sum = vmtool.sumInstanceSize(Object.class);
            long cost = System.currentTimeMillis() - start;
            totalTime.addAndGet(cost);
            System.out.println(i + " sum:" + sum + ", cost " + cost + "ms avgCost " + totalTime.doubleValue() / i + "ms");
        }
    }

    @Test
    public void testCountInstancesMemoryLeak() {
        //这里睡20s是为了方便用jprofiler连接上进程
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        VmTool vmtool = initVmTool();
        final AtomicLong totalTime = new AtomicLong();
        //本地测试请改成200000
        for (int i = 1; i <= 2; i++) {
            long start = System.currentTimeMillis();
            long count = vmtool.countInstances(Object.class);
            long cost = System.currentTimeMillis() - start;
            totalTime.addAndGet(cost);
            System.out.println(i + " count:" + count + ", cost " + cost + "ms avgCost " + totalTime.doubleValue() / i + "ms");
        }
    }

    @Test
    public void testGetAllLoadedClassesMemoryLeak() {
        //这里睡20s是为了方便用jprofiler连接上进程
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        VmTool vmtool = initVmTool();
        final AtomicLong totalTime = new AtomicLong();
        //本地测试请改成200000
        for (int i = 1; i <= 2; i++) {
            long start = System.currentTimeMillis();
            Class<?>[] allLoadedClasses = vmtool.getAllLoadedClasses();
            long cost = System.currentTimeMillis() - start;
            totalTime.addAndGet(cost);
            System.out.println(i + " class size:" + allLoadedClasses.length + ", cost " + cost + "ms avgCost " + totalTime.doubleValue() / i + "ms");
            allLoadedClasses = null;
        }
    }

    @Test
    public void testLimit() {
        VmTool vmtool = initVmTool();
        Object[] instances = vmtool.getInstances(Object.class, 10);
        Assert.assertEquals(10, instances.length);
        instances = vmtool.getInstances(Object.class);
        Assert.assertTrue(instances.length > 0);
    }

    @Test
    public void testSynchronizedGetInstancesPerformance() {
        //性能测试
        VmTool vmtool = initVmTool();

        long total = 0;
        for (int i = 0; i < ROUND; i++) {
            long start = System.nanoTime();
            //测试原方法，需要把arthas.VmTool#getInstances的synchronized去掉
            vmtool.getInstances(Object.class);
            long cost = System.nanoTime() - start;
            System.out.println(i + " cost " + cost + " ns");
            total = total + cost;
        }
        System.out.println("total cost " + total + " ns");

        long synchronizedTotal = 0;
        for (int i = 0; i < ROUND; i++) {
            long start = System.currentTimeMillis();
            synchronized (VmTool.class) {
                vmtool.getInstances(Object.class);
            }
            long cost = System.currentTimeMillis() - start;
            System.out.println(i + " cost " + cost + " ns");
            synchronizedTotal = synchronizedTotal + cost;
        }
        System.out.println("synchronized total cost " + synchronizedTotal + " ms");
        System.out.println("\ngetInstances");
        System.out.println("total1 cost " + total + " ns");
        System.out.println("total2 cost " + synchronizedTotal + " ns");
    }

    @Test
    public void testSynchronizedSumInstanceSizePerformance() {
        //性能测试
        VmTool vmtool = initVmTool();
        long total = 0;
        for (int i = 0; i < ROUND; i++) {
            long start = System.nanoTime();
            //测试原方法，需要把arthas.VmTool#sumInstanceSize的synchronized去掉
            vmtool.sumInstanceSize(Object.class);
            long cost = System.nanoTime() - start;
            System.out.println(i + " cost " + cost + " ns");
            total = total + cost;
        }
        System.out.println("total cost " + total + " ns");

        long synchronizedTotal = 0;
        for (int i = 0; i < ROUND; i++) {
            long start = System.currentTimeMillis();
            synchronized (VmTool.class) {
                vmtool.sumInstanceSize(Object.class);
            }
            long cost = System.currentTimeMillis() - start;
            System.out.println(i + " cost " + cost + " ns");
            synchronizedTotal = synchronizedTotal + cost;
        }
        System.out.println("synchronized total cost " + synchronizedTotal + " ms");
        System.out.println("\nsumInstanceSize");
        System.out.println("total1 cost " + total + " ns");
        System.out.println("total2 cost " + synchronizedTotal + " ns");
    }

    @Test
    public void testSynchronizedCountInstancesPerformance() {
        //性能测试
        VmTool vmtool = initVmTool();
        long total = 0;
        for (int i = 0; i < ROUND; i++) {
            long start = System.nanoTime();
            //测试原方法，需要把arthas.VmTool#countInstances的synchronized去掉
            vmtool.countInstances(Object.class);
            long cost = System.nanoTime() - start;
            System.out.println(i + " cost " + cost + " ns");
            total = total + cost;
        }
        System.out.println("total cost " + total + " ns");

        long synchronizedTotal = 0;
        for (int i = 0; i < ROUND; i++) {
            long start = System.currentTimeMillis();
            synchronized (VmTool.class) {
                vmtool.countInstances(Object.class);
            }
            long cost = System.currentTimeMillis() - start;
            System.out.println(i + " cost " + cost + " ns");
            synchronizedTotal = synchronizedTotal + cost;
        }
        System.out.println("synchronized total cost " + synchronizedTotal + " ms");
        System.out.println("\ncountInstances");
        System.out.println("total1 cost " + total + " ns");
        System.out.println("total2 cost " + synchronizedTotal + " ns");
    }

    @Test
    public void testSynchronizedGetAllLoadedClassesPerformance() {
        //性能测试
        VmTool vmtool = initVmTool();
        long total = 0;
        for (int i = 0; i < ROUND; i++) {
            long start = System.nanoTime();
            //测试原方法，需要把arthas.VmTool#getAllLoadedClasses的synchronized去掉
            vmtool.getAllLoadedClasses();
            long cost = System.nanoTime() - start;
            System.out.println(i + " cost " + cost + " ns");
            total = total + cost;
        }
        System.out.println("total cost " + total + " ns");

        long synchronizedTotal = 0;
        for (int i = 0; i < ROUND; i++) {
            long start = System.currentTimeMillis();
            synchronized (VmTool.class) {
                vmtool.getAllLoadedClasses();
            }
            long cost = System.currentTimeMillis() - start;
            System.out.println(i + " cost " + cost + " ns");
            synchronizedTotal = synchronizedTotal + cost;
        }
        System.out.println("synchronized total cost " + synchronizedTotal + " ms");
        System.out.println("\ngetAllLoadedClasses");
        System.out.println("total1 cost " + total + " ns");
        System.out.println("total2 cost " + synchronizedTotal + " ns");
    }
}
