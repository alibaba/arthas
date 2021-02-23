package com.vdian.vclub;

import com.taobao.arthas.common.AnsiLog;
import org.scijava.nativelib.NativeLoader;

import java.util.LinkedList;

/**
 * 如果是web环境，需要调用init方法。
 *
 * @author 1936978077@qq.com
 * @date 2021/02/12
 */
public class JvmUtils {

    private final static String LIB_NAME = "ArthasJniLibrary";

    static {
        try {
            NativeLoader.loadLibrary(LIB_NAME);
            AnsiLog.warn("checkResult->" + check() + ", jni-lib available !");
        } catch (Throwable t) {
            AnsiLog.error("load jni-lib failed:" + t.getMessage(), t);
        }
    }

    /**
     * 检测jni-lib是否正常，如果正常，应该输出OK
     */
    public static native String check();

    /**
     * 获取某个class在jvm中当前所有存活实例
     */
    public static synchronized native <T> LinkedList<T> getInstances(Class<T> klass);

    /**
     * 统计某个class在jvm中当前所有存活实例的总占用内存，单位：Byte
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

}
