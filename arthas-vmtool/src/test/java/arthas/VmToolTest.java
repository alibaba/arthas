package arthas;

import org.junit.Test;

import com.taobao.arthas.common.VmToolUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 以下本地测试的jvm参数均为：-Xms128m -Xmx128m
 */
public class VmToolTest {

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
            System.out.println("before instances->" + beforeInstances);
            System.out.println("size->" + vmtool.getInstanceSize(weakReference1.get()));
            System.out.println("count->" + vmtool.countInstances(VmTool.class));
            System.out.println("sum size->" + vmtool.sumInstanceSize(VmTool.class));
            beforeInstances = null;

            System.gc();
            Thread.sleep(100);
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            VmTool[] afterInstances = vmtool.getInstances(VmTool.class);
            System.out.println("after instances->" + afterInstances);
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
            System.gc();
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
}
