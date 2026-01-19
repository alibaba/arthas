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

    /**
     * 打断指定线程
     *
     * @param threadId 线程ID
     */
    void interruptSpecialThread(int threadId);

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

    /**
     * glibc 释放空闲内存
     */
    public int mallocTrim();

    /**
     * glibc 输出内存状态到应用的 stderr
     */
    public boolean mallocStats();

    /**
     * 分析堆内存占用最大的对象与类（从 GC Root 可达对象出发）。
     *
     * @param classNum  需要展示的类数量
     * @param objectNum 需要展示的对象数量
     * @return 分析结果文本
     */
    public String heapAnalyze(int classNum, int objectNum);

    /**
     * 分析某个类的实例对象，并输出占用最大的若干对象及其引用回溯链（从对象回溯到 GC Root）。
     *
     * @param klass         目标类
     * @param objectNum     需要展示的对象数量
     * @param backtraceNum  回溯层数，-1 表示一直回溯到 root，0 表示不输出引用链
     * @return 分析结果文本
     */
    public String referenceAnalyze(Class<?> klass, int objectNum, int backtraceNum);
}
