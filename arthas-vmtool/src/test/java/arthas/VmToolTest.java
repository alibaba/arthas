package arthas;

import org.junit.Test;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

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
    public void test01() {
        try {
            String path = VmTool.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            System.err.println(path);

            String libPath = new File(path, VmTool.detectLibName()).getAbsolutePath();
            VmTool vmtool = VmTool.getInstance(libPath);

            //调用native方法，获取已加载的类，不包括小类型(如int)
            ArrayList<Class<?>> allLoadedClasses = vmtool.getAllLoadedClasses();
            System.out.println("allLoadedClasses->" + allLoadedClasses.size());

            //通过下面的例子，可以看到getInstances(Class<T> klass)拿到的是当前存活的所有对象
            WeakReference<VmToolTest> weakReference1 = new WeakReference<VmToolTest>(new VmToolTest());
            WeakReference<VmToolTest> weakReference2 = new WeakReference<VmToolTest>(new VmToolTest());
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            ArrayList<VmTool> beforeInstances = vmtool.getInstances(VmTool.class);
            System.out.println("before instances->" + beforeInstances);
            System.out.println("size->" + vmtool.getInstanceSize(weakReference1.get()));
            System.out.println("count->" + vmtool.countInstances(VmTool.class));
            System.out.println("sum size->" + vmtool.sumInstanceSize(VmTool.class));
            beforeInstances = null;

            System.gc();
            Thread.sleep(100);
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            ArrayList<VmTool> afterInstances = vmtool.getInstances(VmTool.class);
            System.out.println("after instances->" + afterInstances);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
