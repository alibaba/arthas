package arthas;

/**
 * VmTool interface for JMX server. How to register VmTool MBean:
 *
 * <pre>
 * {@code
 *     ManagementFactory.getPlatformMBeanServer().registerMBean(
 *             VmTool.getInstance(),
 *             new ObjectName("arthas:type=VmTool")
 *     );
 * }
 * </pre>
 *
 * @author hengyunabc 2021-04-26
 * @author ZhangZiCheng 2021-04-30
 */
public interface VmToolMXBean {
    /**
     * 检测jni-lib是否正常，如果正常，应该输出OK
     */
    public String check();

    /**
     * https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#ForceGarbageCollection
     */
    public void forceGc();

    /**
     * 获取某个class在jvm中当前所有存活实例
     */
    public <T> T[] getInstances(Class<T> klass);

    /**
     * 获取某个class在jvm中当前存活实例
     *
     * @param klass 类的类型
     * @param limit 如果为-1，取所有的；
     *              小于-1，将抛出{@link IllegalArgumentException}；
     *              其他情况取limit数量的存活实例
     */
    public <T> T[] getInstances(Class<T> klass, int limit);

    /**
     * 统计某个class在jvm中当前所有存活实例的总占用内存，单位：Byte
     */
    public long sumInstanceSize(Class<?> klass);

    /**
     * 获取某个实例的占用内存，单位：Byte
     */
    public long getInstanceSize(Object instance);

    /**
     * 统计某个class在jvm中当前所有存活实例的总个数
     */
    public long countInstances(Class<?> klass);

    /**
     * 获取所有已加载的类
     */
    public Class<?>[] getAllLoadedClasses();
}
