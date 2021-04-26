package arthas;

import java.util.ArrayList;

/**
 * @author ZhangZiCheng 2021-02-12
 * @author hengyunabc 2021-04-26
 * @since 3.5.1
 */
public class Vmtool {

    /**
     * 不要修改jni-lib的名称
     */
    public final static String JNI_LIBRARY_NAME = "ArthasJniLibrary";

    private static Vmtool instance;

    private Vmtool() {
    }

    public static Vmtool getInstance() {
        return getInstance(null);
    }

    public static synchronized Vmtool getInstance(String libPath) {
        if (instance != null) {
            return instance;
        }

        if (libPath == null) {
            System.loadLibrary(JNI_LIBRARY_NAME);
        } else {
            System.load(libPath);
        }

        instance = new Vmtool();
        return instance;
    }

    /**
     * 检测jni-lib是否正常，如果正常，应该输出OK
     */
    public static native String check();

    /**
     * 获取某个class在jvm中当前所有存活实例
     */
    public static native <T> ArrayList<T> getInstances(Class<T> klass);

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
    public static native ArrayList<Class<?>> getAllLoadedClasses();

    /**
     * 包括小类型(如int)
     */
    @SuppressWarnings("all")
    public static ArrayList<Class> getAllClasses() {
        return getInstances(Class.class);
    }

}
