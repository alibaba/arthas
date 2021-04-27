package arthas;

import org.junit.Test;

import arthas.Vmtool;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class VmtoolTest {

    /**
     * 在我的macbook上运行结果如下
     * allLoadedClasses->1050
     * arthas.JvmUtils@5bb21b69 arthas.JvmUtils@6b9651f3
     * before instances->[arthas.JvmUtils@5bb21b69, arthas.JvmUtils@6b9651f3]
     * size->16
     * count->2
     * sum size->32
     * null null
     * after instances->[]
     */
    @Test
    public void test01() {
        try {
            String path = Vmtool.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            System.err.println(path);

            String libPath = new File(path, Vmtool.detectLibName()).getAbsolutePath();
            Vmtool vmtool = Vmtool.getInstance(libPath);

            //调用native方法，获取已加载的类，不包括小类型(如int)
            ArrayList<Class<?>> allLoadedClasses = vmtool.getAllLoadedClasses();
            System.out.println("allLoadedClasses->" + allLoadedClasses.size());

            //通过下面的例子，可以看到getInstances(Class<T> klass)拿到的是当前存活的所有对象
            WeakReference<VmtoolTest> weakReference1 = new WeakReference<VmtoolTest>(new VmtoolTest());
            WeakReference<VmtoolTest> weakReference2 = new WeakReference<VmtoolTest>(new VmtoolTest());
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            ArrayList<Vmtool> beforeInstances = vmtool.getInstances(Vmtool.class);
            System.out.println("before instances->" + beforeInstances);
            System.out.println("size->" + vmtool.getInstanceSize(weakReference1.get()));
            System.out.println("count->" + vmtool.countInstances(Vmtool.class));
            System.out.println("sum size->" + vmtool.sumInstanceSize(Vmtool.class));
            beforeInstances = null;

            System.gc();
            Thread.sleep(100);
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            ArrayList<Vmtool> afterInstances = vmtool.getInstances(Vmtool.class);
            System.out.println("after instances->" + afterInstances);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
