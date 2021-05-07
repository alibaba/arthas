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
     */
    private static native <T> T[] getInstances0(Class<T> klass);

    /**
     * 统计某个class在jvm中当前所有存活实例的总占用内存，单位：Byte
     */
    private static native long sumInstanceSize0(Class<?> klass);

    /**
     * 获取某个实例的占用内存，单位：Byte
     */
    private static native long getInstanceSize0(Object instance);

    /**
     * 统计某个class在jvm中当前所有存活实例的总个数
     */
    private static native long countInstances0(Class<?> klass);

    /**
     * 获取所有已加载的类
     * @param klass 这个参数必须是 Class.class
     * @return
     */
    private static native Class<?>[] getAllLoadedClasses0(Class<?> klass);

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
        return getInstances0(klass);
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
        return getAllLoadedClasses0(Class.class);
    }

}
