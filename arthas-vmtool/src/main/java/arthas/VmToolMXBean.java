package arthas;

/**
 * VmToolMXBean接口 - VmTool的JMX接口
 *
 * 该接口定义了VmTool类的公开方法，使其可以作为JMX MBean注册和管理。
 * 通过JMX（Java Management Extensions），可以在运行时监控和管理JVM。
 *
 * 注册VmTool MBean的方法：
 *
 * <pre>
 * {@code
 *     // 获取平台MBean服务器
 *     MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
 *
 *     // 创建ObjectName，指定MBean的名称
 *     ObjectName name = new ObjectName("arthas:type=VmTool");
 *
 *     // 注册VmTool MBean
 *     mbs.registerMBean(
 *         VmTool.getInstance(),  // MBean实例
 *         name                   // MBean名称
 *     );
 *
 *     // 之后可以使用JConsole或其他JMX客户端连接和管理
 * }
 * </pre>
 *
 * 参考文档：
 * https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#ForceGarbageCollection
 *
 * @author hengyunabc 2021-04-26
 */
public interface VmToolMXBean {

    /**
     * 强制执行垃圾回收
     *
     * 该方法请求JVM执行一次完整的垃圾回收操作。
     * 对应JVMTI（JVM Tool Interface）中的ForceGarbageCollection函数。
     *
     * 注意事项：
     * - 该方法只是"建议"JVM执行GC，JVM可能会忽略请求
     * - 执行GC会导致STW（Stop-The-World）暂停
     * - 频繁调用可能影响应用程序性能
     * - 不能保证回收所有不可达对象
     *
     * 使用场景：
     * - 诊断内存问题
     * - 测试内存管理行为
     * - 在生产环境中谨慎使用
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#ForceGarbageCollection">
     *     JVMTI ForceGarbageCollection文档</a>
     */
    public void forceGc();

    /**
     * 打断指定线程
     *
     * 该方法通过线程ID找到对应的线程，并调用其interrupt()方法。
     *
     * 实现细节：
     * - 遍历所有活动线程，查找ID匹配的线程
     * - 调用Thread.interrupt()设置中断标志
     * - 不会强制终止线程，只是设置中断标志
     *
     * 线程中断的处理：
     * - 如果线程正在调用wait()、join()、sleep()等方法，会抛出InterruptedException
     * - 线程需要通过Thread.interrupted()或isInterrupted()检查中断状态
     * - 线程应该根据中断状态决定如何响应
     *
     * @param threadId 要打断的线程ID
     */
    void interruptSpecialThread(int threadId);

    /**
     * 获取某个类的所有存活实例（不限制数量）
     *
     * 该方法返回JVM中特定类的所有存活对象实例。
     * "存活"指的是被GC Root可达的对象。
     *
     * 注意事项：
     * - 如果对象数量很大，可能会消耗大量内存
     * - 返回的数组包含实际的对象引用
     * - 可能触发GC以保证返回的都是存活对象
     *
     * @param <T> 对象的类型参数
     * @param klass 要查询的Class对象
     * @return 该类的所有存活实例数组
     */
    public <T> T[] getInstances(Class<T> klass);

    /**
     * 获取某个类的所有存活实例（可限制数量）
     *
     * 该方法返回JVM中特定类的存活对象实例，可以限制返回的最大数量。
     * "存活"指的是被GC Root可达的对象。
     *
     * 参数说明：
     * - klass：要查询的类
     * - limit：返回实例的最大数量
     *   - 如果小于0：不限制返回数量
     *   - 如果等于0：会抛出IllegalArgumentException
     *   - 如果大于0：最多返回limit个实例
     *
     * 使用场景：
     * - 诊断内存泄漏
     * - 分析对象分布
     * - 调试缓存问题
     *
     * @param <T> 对象的类型参数
     * @param klass 要查询的Class对象
     * @param limit 返回实例的最大数量，如果小于0则不限制
     * @return 该类的存活实例数组（最多返回limit个）
     */
    public <T> T[] getInstances(Class<T> klass, int limit);

    /**
     * 统计某个类的所有存活实例的总内存占用
     *
     * 该方法计算JVM中特定类的所有存活对象占用的堆内存总量。
     * 计算包括：
     * - 对象头（Object Header）
     * - 实例字段（Instance Fields）
     * - 数组元素（Array Elements，如果是数组）
     * - 对齐填充（Padding）
     *
     * 注意：
     * - 包括对象引用的其他对象的大小（深大小）
     * - 不同JVM实现可能有不同的计算方式
     * - 大对象图可能会消耗较多计算时间
     *
     * 使用场景：
     * - 评估内存使用情况
     * - 发现内存泄漏
     * - 优化内存分配
     *
     * @param klass 要统计的Class对象
     * @return 总内存占用（单位：字节）
     */
    public long sumInstanceSize(Class<?> klass);

    /**
     * 获取单个对象的内存占用大小
     *
     * 该方法返回指定对象在堆内存中的占用大小（浅大小）。
     * 计算包括：
     * - 对象头（Object Header）
     * - 实例字段（Instance Fields）
     * - 对齐填充（Padding）
     *
     * 注意：
     * - 不包括对象引用的其他对象的大小
     * - 不同JVM实现可能有不同的对象布局
     * - 压缩指针（Compressed Oops）会影响对象大小
     *
     * @param instance 要计算的对象实例
     * @return 对象的内存占用大小（单位：字节）
     */
    public long getInstanceSize(Object instance);

    /**
     * 统计某个类的所有存活实例数量
     *
     * 该方法返回JVM中特定类的存活对象实例总数。
     * "存活"指的是被GC Root可达的对象。
     *
     * 使用场景：
     * - 检测对象泄漏
     * - 监控缓存大小
     * - 分析对象生命周期
     * - 评估GC效果
     *
     * @param klass 要统计的Class对象
     * @return 该类的存活实例总数
     */
    public long countInstances(Class<?> klass);

    /**
     * 获取所有已加载的类
     *
     * 该方法返回JVM中所有已加载的类的Class对象数组。
     * 包括：
     * - Java核心类（java.lang.*, java.util.*等）
     * - 应用类
     * - 第三方库类
     * - 动态代理类
     * - Lambda表达式生成的类
     *
     * 注意：
     * - 不包括已被卸载的类
     * - 大型应用可能有数千个类
     *
     * 使用场景：
     * - 类加载分析
     * - 类泄漏检测
     * - 类结构探索
     *
     * @return 所有已加载的Class对象数组
     */
    public Class<?>[] getAllLoadedClasses();

    /**
     * glibc释放空闲内存
     *
     * 该方法调用glibc的malloc_trim()函数，将未使用的内存归还给操作系统。
     *
     * 功能说明：
     * - 遍历堆的顶部区域，查找连续的空闲内存块
     * - 将这些空闲内存释放给操作系统
     * - 减少进程的虚拟内存占用
     *
     * 返回值：
     * - > 0：成功释放了内存，返回释放的内存页数
     * - = 0：没有可释放的内存或释放失败
     * - < 0：操作失败
     *
     * 注意事项：
     * - 仅适用于使用glibc malloc的系统（主要是Linux）
     * - 频繁调用可能影响性能
     * - 不一定立即减少物理内存占用
     *
     * @return 操作结果码
     */
    public int mallocTrim();

    /**
     * glibc输出内存状态到应用的stderr
     *
     * 该方法调用glibc的malloc_stats()函数，输出内存分配统计信息。
     *
     * 输出信息包括：
     * - 分配的内存总量（allocated）
     * - 可用的内存总量（available）
     * - 内存块的数量和大小分布
     * - mmap分配的内存信息
     *
     * 输出格式示例：
     * <pre>
     * Arena 0:
     * system bytes     = 135168
     * in use bytes     = 32
     * Total (incl. mmap):
     * system bytes     = 135168
     * in use bytes     = 32
     * max mmap regions = 65536
     * max mmap bytes   = 6291456000
     * </pre>
     *
     * 注意事项：
     * - 仅适用于使用glibc malloc的系统
     * - 输出到stderr，不影响标准输出
     * - 主要用于诊断和调试
     *
     * @return 操作是否成功
     */
    public boolean mallocStats();

    /**
     * 分析堆内存占用最大的对象与类（从GC Root可达对象出发）
     *
     * 该方法分析Java堆内存使用情况，找出内存占用最大的对象和类。
     * 分析从GC Root（垃圾回收根对象）开始遍历所有可达对象。
     *
     * GC Root包括：
     * - 静态变量（Class对象）
     * - 活动线程的栈变量
     * - JNI本地引用
     * - 系统类加载器加载的类
     *
     * 参数说明：
     * @param classNum 需要展示的类数量
     *        - 按内存占用降序排列
     *        - 取前N个占用最多的类
     * @param objectNum 需要展示的对象数量
     *        - 按内存占用降序排列
     *        - 取前N个占用最多的对象
     *
     * 返回结果包括：
     * - 类名和实例数量
     * - 类的总内存占用
     * - 单个对象的内存占用
     * - 对象的详细信息（对于大对象）
     *
     * @return 分析结果的文本描述，格式化输出便于阅读
     */
    public String heapAnalyze(int classNum, int objectNum);

    /**
     * 分析某个类的实例对象，并输出占用最大的若干对象及其引用回溯链（从对象回溯到GC Root）
     *
     * 该方法分析特定类的实例，找出内存占用最大的对象，
     * 并追踪它们到GC Root的完整引用链。
     *
     * 引用链的作用：
     * - 了解对象为何无法被GC回收
     * - 找到持有该对象的代码位置
     * - 诊断内存泄漏的根本原因
     *
     * 参数说明：
     * @param klass 目标类，要分析的对象类型
     * @param objectNum 需要展示的对象数量
     *        - 按内存占用降序排列
     *        - 取前N个占用最多的对象
     * @param backtraceNum 回溯层数，控制引用链的深度：
     *        - -1：一直回溯到GC Root（完整链）
     *        -  0：不输出引用链（仅对象信息）
     *        -  N：回溯N层引用
     *
     * 返回结果包括：
     * - 对象的内存占用
     * - 对象的引用链（按层数输出）
     * - 每层引用的类型和位置
     * - GC Root的类型
     *
     * 引用类型标识：
     * - 强引用（Strong Reference）
     * - 软引用（Soft Reference）
     * - 弱引用（Weak Reference）
     * - 虚引用（Phantom Reference）
     *
     * @return 分析结果文本，包含对象信息和引用链
     */
    public String referenceAnalyze(Class<?> klass, int objectNum, int backtraceNum);
}
