package com.vdian.vclub;

import com.taobao.arthas.common.AnsiLog;
import org.scijava.nativelib.NativeLoader;

/**
 * @author ZhangZiCheng 2021-02-12
 * @since 3.5.1
 */
public class JvmUtils {

    /**
     * 不要修改jni-lib的名称
     */
    private final static String JNI_LIBRARY_NAME = "ArthasJniLibrary";

    static {
        try {
            NativeLoader.loadLibrary(JNI_LIBRARY_NAME);
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
    public static native <T> T[] getInstances(Class<T> klass);

    /**
     * 统计某个class在jvm中当前所有存活实例的总占用内存，单位：Byte
     */
    public static native long sumInstanceSize(Class<?> klass);

    /**
     * 获取某个实例的占用内存，单位：Byte
     */
    public static native long getInstanceSize(Object instance);

    /**
     * 统计某个class在jvm中当前所有存活实例的总个数
     */
    public static native long countInstances(Class<?> klass);

    /**
     * 获取所有已加载的类
     */
    public static native Class<?>[] getAllLoadedClasses();

    /**
     * 包括小类型(如int)
     */
    public static Class<?>[] getAllClasses() {
        return getInstances(Class.class);
    }

}
