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

    private static synchronized native void forceGc0();

    /**
     * 获取某个class在jvm中当前所有存活实例
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
     * @param klass 这个参数必须是 Class.class
     * @return
     */
    private static synchronized native Class<?>[] getAllLoadedClasses0(Class<?> klass);
    private static native Object[] getLocalVariableTable0(Thread jthread,int depth);
    private static native int getLocalInt0(Thread jthread,int depth,int slot);
    private static native long getLocalLong0(Thread jthread,int depth,int slot);
    private static native float getLocalFloat0(Thread jthread,int depth,int slot);
    private static native double getLocalDouble0(Thread jthread,int depth,int slot);
    private static native Object getLocalObject0(Thread jthread,int depth,int slot);
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
        if (limit == 0) {
            throw new IllegalArgumentException("limit can not be 0");
        }
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
        return getAllLoadedClasses0(Class.class);
    }

    @Override
    public Object[] getLocalVariableTable(Thread jthread,int depth){
        return getLocalVariableTable0(jthread,depth);
    }

    @Override
    public int getLocalInt(Thread jthread,int depth,int slot) {
        return getLocalInt0(jthread,depth,slot);
    }

    @Override
    public long getLocalLong(Thread jthread,int depth,int slot) {
        return getLocalLong0(jthread,depth,slot);
    }

    @Override
    public float getLocalFloat(Thread jthread,int depth,int slot) {
        return getLocalFloat0(jthread,depth,slot);
    }

    @Override
    public double getLocalDouble(Thread jthread,int depth,int slot) {
        return getLocalDouble0(jthread,depth,slot);
    }

    @Override
    public Object getLocalObject(Thread jthread,int depth,int slot) {
        return getLocalObject0(jthread,depth,slot);
    }
}
