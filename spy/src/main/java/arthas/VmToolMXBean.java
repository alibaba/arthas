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
 * @author hengyunabc 2021-04-26
 */
public interface VmToolMXBean {

    /**
     * https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#ForceGarbageCollection
     */
    public void forceGc();

    public <T> T[] getInstances(Class<T> klass);

    /**
     * 获取某个class在jvm中当前所有存活实例
     * @param <T>
     * @param klass
     * @param limit 如果小于 0 ，则不限制
     * @return
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
