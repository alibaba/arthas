package com.vdian.vclub;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

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
            LinkedList<Class<?>> allLoadedClasses = JvmUtils.getAllLoadedClasses();
            System.out.println("allLoadedClasses->" + allLoadedClasses.size());

            //通过下面的例子，可以看到getInstances(Class<T> klass)拿到的是当前存活的所有对象
            WeakReference<JvmUtils> weakReference1 = new WeakReference<JvmUtils>(new JvmUtils());
            WeakReference<JvmUtils> weakReference2 = new WeakReference<JvmUtils>(new JvmUtils());
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            LinkedList<JvmUtils> beforeInstances = JvmUtils.getInstances(JvmUtils.class);
            System.out.println("before instances->" + beforeInstances);
            System.out.println("size->" + JvmUtils.getInstanceSize(weakReference1.get()));
            System.out.println("count->" + JvmUtils.countInstances(JvmUtils.class));
            System.out.println("sum size->" + JvmUtils.sumInstanceSize(JvmUtils.class));
            beforeInstances = null;

            System.gc();
            Thread.sleep(100);
            System.out.println(weakReference1.get() + " " + weakReference2.get());
            LinkedList<JvmUtils> afterInstances = JvmUtils.getInstances(JvmUtils.class);
            System.out.println("after instances->" + afterInstances);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
