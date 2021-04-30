package arthas;

/**
 * @author ZhangZiCheng 2021-02-12
 * @author hengyunabc 2021-04-26
 * @since 3.5.1
 */
public class VmTool implements VmToolMXBean {

    /**
     * 不要修改jni-lib的名称
     */
    public final static String JNI_LIBRARY_NAME = "ArthasJniLibrary";

    private static VmTool instance;

    private VmTool() {
    }

    public static VmTool getInstance() {
        return getInstance(null);
    }

    public static synchronized VmTool getInstance(String libPath) {
        if (instance != null) {
            return instance;
        }

        if (libPath == null) {
            System.loadLibrary(JNI_LIBRARY_NAME);
        } else {
            System.load(libPath);
        }

        instance = new VmTool();
        return instance;
    }

    /**
     * 检测jni-lib是否正常，如果正常，应该输出OK
     */
    private static native String check0();

    private static native void forceGc0();

    /**
     * 获取某个class在jvm中当前所有存活实例
     *
     * @param klass 类的类型
     * @param limit 如果为-1，取所有的；
     *              小于-1，将抛出{@link IllegalArgumentException}；
     *              其他情况取limit数量的存活实例
     */
    private static synchronized native <T> T[] getInstances0(Class<T> klass, int limit);

    /**
     * 统计某个class在jvm中当前所有存活实例的总占用内存，单位：Byte
     */
    private static synchronized native long sumInstanceSize0(Class<?> klass);

    /**
     * 获取某个实例的占用内存，单位：Byte
     */
    private static native long getInstanceSize0(Object instance);

    /**
     * 统计某个class在jvm中当前所有存活实例的总个数
     */
    private static synchronized native long countInstances0(Class<?> klass);

    /**
     * 获取所有已加载的类
     */
    private static synchronized native Class<?>[] getAllLoadedClasses0();

    /**
     * 包括小类型(如int)
     */
    public static Class<?>[] getAllClasses() {
        return getInstances0(Class.class, -1);
    }

    @Override
    public String check() {
        return check0();
    }

    @Override
    public void forceGc() {
        forceGc0();
    }

    @Override
    public <T> T[] getInstances(Class<T> klass) {
        return getInstances0(klass, -1);
    }

    @Override
    public <T> T[] getInstances(Class<T> klass, int limit) {
        return getInstances0(klass, limit);
    }

    @Override
    public long sumInstanceSize(Class<?> klass) {
        return sumInstanceSize0(klass);
    }

    @Override
    public long getInstanceSize(Object instance) {
        return getInstanceSize0(instance);
    }

    @Override
    public long countInstances(Class<?> klass) {
        return countInstances0(klass);
    }

    @Override
    public Class<?>[] getAllLoadedClasses() {
        return getAllLoadedClasses0();
    }

}
