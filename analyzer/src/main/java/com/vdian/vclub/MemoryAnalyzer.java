package com.vdian.vclub;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
        String classPath = ClassLoader.getSystemClassLoader().getResource("").getPath();
        System.load(classPath + "jni-lib.so");
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
    public static List<MemoryInfo> analyze(boolean skipZero) {
        LinkedList<Class<?>> loadedClasses = getAllLoadedClasses();
        List<MemoryInfo> list = new LinkedList<>();
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
        List<MemoryInfo> result = new LinkedList<>();
        for (MemoryInfo info : list) {
            double percentage = new BigDecimal(info.getSumSize())
                    .divide(sumDecimal, 2, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
            //过滤几乎没有影响的class
            if (skipZero && percentage == 0) {
                continue;
            }
            info.setPercentage(percentage);
            result.add(info);
        }
        return result.stream()
                .sorted(Comparator.comparingLong(MemoryInfo::getSumSize)
                        .reversed())
                .collect(Collectors.toList());
    }
}
