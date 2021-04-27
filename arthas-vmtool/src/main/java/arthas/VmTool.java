package arthas;

import java.util.ArrayList;

import com.taobao.arthas.common.OSUtils;

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

    private static String libName = null;
    static {
        if (OSUtils.isMac()) {
            libName = "libArthasJniLibrary-x64.dylib";
        }
        if (OSUtils.isLinux()) {
            libName = "libArthasJniLibrary-x64.so";
            if (OSUtils.isArm32()) {
                libName = "libArthasJniLibrary-arm.so";
            } else if (OSUtils.isArm64()) {
                libName = "libArthasJniLibrary-aarch64.so";
            }
        }
        if (OSUtils.isWindows()) {
            libName = "libArthasJniLibrary-x64.dll";
        }
    }

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

    public static String detectLibName() {
        return libName;
    }

    /**
     * 检测jni-lib是否正常，如果正常，应该输出OK
     */
    private static native String check0();

    /**
     * 获取某个class在jvm中当前所有存活实例
     */
    private static native <T> ArrayList<T> getInstances0(Class<T> klass);

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
     */
    private static native ArrayList<Class<?>> getAllLoadedClasses0();

    /**
     * 包括小类型(如int)
     */
    @SuppressWarnings("all")
    public static ArrayList<Class> getAllClasses() {
        return getInstances0(Class.class);
    }

    @Override
    public String check() {
        return check0();
    }

    @Override
    public <T> ArrayList<T> getInstances(Class<T> klass) {
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
    public ArrayList<Class<?>> getAllLoadedClasses() {
        return getAllLoadedClasses0();
    }

}
