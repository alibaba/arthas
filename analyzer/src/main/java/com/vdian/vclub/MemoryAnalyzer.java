package com.vdian.vclub;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * 内存分析器，能够在不dump的前提下，获取：
 * 1.某个class在jvm中当前所有存活实例{@link #getInstances(java.lang.Class)};
 * 2.某个class在jvm中当前所有存活实例的总占用内存{@link #sumInstanceSize(java.lang.Class)};
 * 3.某个实例的占用内存{@link #getInstanceSize(java.lang.Object)};
 * 4.某个class在jvm中当前所有存活实例的总个数{@link #countInstances(java.lang.Class)};
 * 5.所有已加载的类(不包括void、int、boolean、float等小类型){@link #getAllLoadedClasses()};
 * 6.系统中占用内存最多的类及具体KB{@link #analyze(boolean)}.
 *
 * @author zhangzicheng
 * @date 2020/1/16
 */
public class MemoryAnalyzer {

    static {
        //加载c++类库
        URL classPath = ClassLoader.getSystemClassLoader().getResource("");
        if (null == classPath) {
            throw new RuntimeException("class path not found !");
        }
        System.load(classPath.getPath() + "jni-lib.so");
    }

    /**
     * 获取某个class在jvm中当前所有存活实例
     */
    public static synchronized native <T> LinkedList<T> getInstances(Class<T> klass);

    /**
     * 统计某个class在jvm中当前所有存活实例的总占用内存
     */
    public static synchronized native long sumInstanceSize(Class<?> klass);

    /**
     * 获取某个实例的占用内存，单位：Byte
     */
    public static native long getInstanceSize(Object instance);

    /**
     * 统计某个class在jvm中当前所有存活实例的总个数
     */
    public static synchronized native long countInstances(Class<?> klass);

    /**
     * 获取所有已加载的类
     */
    public static native LinkedList<Class<?>> getAllLoadedClasses();

    /**
     * 包括小类型(如int)
     */
    @SuppressWarnings("all")
    public static LinkedList<Class> getAllClasses() {
        return getInstances(Class.class);
    }

    /**
     * 根据类所有实例的总大小，分析jvm中占用内存最多的一些类
     */
    public static List<MemoryInfo> analyze(boolean all) {
        LinkedList<Class<?>> loadedClasses = getAllLoadedClasses();
        List<MemoryInfo> list = new LinkedList<MemoryInfo>();
        long sum = 0;
        for (Class<?> loadedClass : loadedClasses) {
            if (loadedClass.isInterface()) {
                continue;
            }
            long sumSize = MemoryAnalyzer.sumInstanceSize(loadedClass);
            if (sumSize == 0) {
                continue;
            }
            sum = sum + sumSize;
            list.add(new MemoryInfo(loadedClass, sumSize));
        }
        BigDecimal sumDecimal = new BigDecimal(sum);
        List<MemoryInfo> result = new LinkedList<MemoryInfo>();
        for (MemoryInfo info : list) {
            double percentage = new BigDecimal(info.getSumSize() * 100)
                    .divide(sumDecimal, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            //过滤几乎没有影响的class
            if (!all && percentage == 0) {
                continue;
            }
            info.setPercentage(percentage);
            result.add(info);
        }
        Collections.sort(result, new Comparator<MemoryInfo>() {
            @Override
            public int compare(MemoryInfo o1, MemoryInfo o2) {
                long x = o1.getSumSize();
                long y = o2.getSumSize();
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }
        });
        return result;
    }
}
