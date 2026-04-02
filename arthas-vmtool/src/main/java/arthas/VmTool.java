package arthas;

import java.util.Map;

/**
 * VmTool类 - JVM工具类
 *
 * 该类通过JNI（Java Native Interface）提供对JVM内部功能的访问，
 * 主要用于诊断和分析Java应用程序的运行状态。
 *
 * 核心功能：
 * 1. 强制执行垃圾回收
 * 2. 获取特定类的所有实例
 * 3. 统计实例的内存占用
 * 4. 获取所有已加载的类
 * 5. 分析堆内存占用
 * 6. 分析对象引用链
 * 7. 调用glibc的内存管理函数
 *
 * 该类实现了VmToolMXBean接口，可以作为JMX MBean注册使用。
 *
 * @author ZhangZiCheng 2021-02-12
 * @author hengyunabc 2021-04-26
 * @since 3.5.1
 */
public class VmTool implements VmToolMXBean {

    /**
     * JNI库名称常量
     *
     * 该常量定义了需要加载的本地动态链接库的名称。
     * 在不同操作系统上，实际加载的文件名会不同：
     * - Linux: libArthasJniLibrary.so
     * - Windows: ArthasJniLibrary.dll
     * - macOS: libArthasJniLibrary.dylib
     *
     * 注意：不要修改这个名称，因为它必须与本地编译生成的库名称匹配
     */
    public final static String JNI_LIBRARY_NAME = "ArthasJniLibrary";

    /**
     * VmTool单例实例
     *
     * 该静态变量持有VmTool类的唯一实例。
     * 通过双重检查锁定模式确保在多线程环境下的线程安全性。
     */
    private static VmTool instance;

    /**
     * 私有构造函数
     *
     * 该构造函数为私有，防止外部直接创建实例。
     * 使用单例模式，确保整个JVM中只有一个VmTool实例。
     */
    private VmTool() {
    }

    /**
     * 获取VmTool实例（使用默认库路径）
     *
     * 该方法是获取VmTool实例的便捷方式，会自动从系统库路径中加载JNI库。
     *
     * @return VmTool的单例实例
     */
    public static VmTool getInstance() {
        return getInstance(null);
    }

    /**
     * 获取VmTool实例（可指定库路径）
     *
     * 该方法采用单例模式和延迟初始化策略：
     * 1. 首次调用时会加载JNI库并创建实例
     * 2. 后续调用直接返回已创建的实例
     * 3. 使用synchronized确保多线程安全
     *
     * JNI库加载方式：
     * - 如果libPath为null：调用System.loadLibrary()从系统库路径加载
     * - 如果libPath不为null：调用System.load()从指定路径加载
     *
     * @param libPath JNI库的完整路径，如果为null则使用默认库路径
     * @return VmTool的单例实例
     */
    public static synchronized VmTool getInstance(String libPath) {
        // 如果实例已存在，直接返回
        if (instance != null) {
            return instance;
        }

        // 首次调用时加载JNI库
        if (libPath == null) {
            // 从系统库路径加载（例如：LD_LIBRARY_PATH环境变量指定的路径）
            System.loadLibrary(JNI_LIBRARY_NAME);
        } else {
            // 从指定的完整路径加载库文件
            System.load(libPath);
        }

        // 创建并保存单例实例
        instance = new VmTool();
        return instance;
    }

    /**
     * 强制执行垃圾回收（本地方法）
     *
     * 该方法调用JVM的垃圾回收机制，强制执行一次完整的GC。
     * 对应JVMTI（JVM Tool Interface）中的ForceGarbageCollection函数。
     *
     * 使用synchronized确保在同一时间只有一个线程能调用GC
     */
    private static synchronized native void forceGc0();

    /**
     * 获取某个类的所有存活实例（本地方法）
     *
     * 该方法通过JVMTI接口获取JVM中特定类的所有存活对象实例。
     * 返回的实例数组可以被Java代码直接访问和操作。
     *
     * 注意：
     * - 只返回存活的对象（即被强引用引用的对象）
     * - limit参数用于限制返回的实例数量，防止内存溢出
     *
     * @param <T> 对象的类型参数
     * @param klass 要查询的Class对象
     * @param limit 返回实例的最大数量，-1表示不限制
     * @return 该类的所有存活实例数组
     */
    private static synchronized native <T> T[] getInstances0(Class<T> klass, int limit);

    /**
     * 统计某个类的所有存活实例的总内存占用（本地方法）
     *
     * 该方法计算JVM中特定类的所有存活对象占用的堆内存总量。
     * 统计范围包括对象本身及其所有引用字段占用的内存。
     *
     * 使用场景：
     * - 诊断内存泄漏
     * - 分析内存占用分布
     * - 评估GC压力
     *
     * @param klass 要统计的Class对象
     * @return 总内存占用（单位：字节）
     */
    private static synchronized native long sumInstanceSize0(Class<?> klass);

    /**
     * 获取单个对象的内存占用大小（本地方法）
     *
     * 该方法计算指定对象实例在堆内存中占用的字节数。
     * 计算包括对象头、实例字段和对齐填充等所有内存开销。
     *
     * 注意：
     * - 不包括对象引用的其他对象的大小（即浅大小）
     * - 不同JVM实现可能有不同的对象布局和计算方式
     *
     * @param instance 要计算的对象实例
     * @return 对象的内存占用大小（单位：字节）
     */
    private static native long getInstanceSize0(Object instance);

    /**
     * 统计某个类的所有存活实例数量（本地方法）
     *
     * 该方法返回JVM中特定类的存活对象实例的总数。
     * 这是分析内存使用情况的重要指标。
     *
     * 使用场景：
     * - 检测对象泄漏
     * - 监控缓存大小
     * - 分析对象生命周期
     *
     * @param klass 要统计的Class对象
     * @return 该类的存活实例总数
     */
    private static synchronized native long countInstances0(Class<?> klass);

    /**
     * 获取所有已加载的类（本地方法）
     *
     * 该方法返回JVM中已加载的所有类的Class对象数组。
     * 这包括：
     * - 系统类（如java.lang.*）
     * - 应用类
     * - 动态生成的类
     *
     * @param klass 该参数必须是Class.class，用于类型标记
     * @return 所有已加载的Class对象数组
     */
    private static synchronized native Class<?>[] getAllLoadedClasses0(Class<?> klass);

    /**
     * 分析堆内存占用最大的对象与类（本地方法）
     *
     * 该方法执行堆内存分析，找出占用内存最大的对象和类。
     * 分析从GC Root可达的对象开始进行遍历。
     *
     * @param classNum 需要展示的类数量（按内存占用排序）
     * @param objectNum 需要展示的对象数量（按内存占用排序）
     * @return 分析结果的文本描述，包含类名、对象数量和内存占用等信息
     */
    private static synchronized native String heapAnalyze0(int classNum, int objectNum);

    /**
     * 分析指定类实例的引用回溯链（本地方法）
     *
     * 该方法分析特定类的实例对象，找出占用内存最大的对象，
     * 并输出这些对象到GC Root的引用链。
     *
     * 使用场景：
     * - 查找内存泄漏的源头
     * - 分析对象的持有关系
     * - 优化内存使用
     *
     * @param klass 目标类
     * @param objectNum 需要展示的对象数量
     * @param backtraceNum 回溯层数：-1表示一直回溯到root，0表示不输出引用链
     * @return 分析结果文本，包含对象信息和引用链
     */
    private static synchronized native String referenceAnalyze0(Class<?> klass, int objectNum, int backtraceNum);

    /**
     * 强制执行垃圾回收
     *
     * 这是VmToolMXBean接口的实现方法，
     * 调用本地方法forceGc0()来执行实际的垃圾回收操作。
     *
     * @see #forceGc0()
     */
    @Override
    public void forceGc() {
        forceGc0();
    }

    /**
     * 打断指定线程
     *
     * 该方法通过线程ID找到对应的线程，并调用其interrupt()方法。
     *
     * 实现逻辑：
     * 1. 获取JVM中所有活动线程的堆栈跟踪
     * 2. 遍历查找ID匹配的线程
     * 3. 找到后调用interrupt()方法打断该线程
     *
     * 注意：该方法不会停止线程，只是设置线程的中断标志
     *
     * @param threadId 要打断的线程ID
     */
    @Override
    public void interruptSpecialThread(int threadId) {
        // 获取所有线程及其堆栈跟踪信息
        Map<Thread, StackTraceElement[]> allThread = Thread.getAllStackTraces();
        // 遍历所有线程
        for (Map.Entry<Thread, StackTraceElement[]> entry : allThread.entrySet()) {
            // 检查线程ID是否匹配
            if (entry.getKey().getId() == threadId) {
                // 调用interrupt()方法打断线程
                entry.getKey().interrupt();
                return;
            }
        }
    }

    /**
     * 获取某个类的所有存活实例（不限制数量）
     *
     * 该方法获取指定类的所有存活实例，不限制返回数量。
     * 内部调用getInstances0()方法，传入-1表示不限制。
     *
     * @param <T> 对象的类型参数
     * @param klass 要查询的Class对象
     * @return 该类的所有存活实例数组
     */
    @Override
    public <T> T[] getInstances(Class<T> klass) {
        return getInstances0(klass, -1);
    }

    /**
     * 获取某个类的所有存活实例（可限制数量）
     *
     * 该方法获取指定类的存活实例，可以限制返回的最大数量。
     * 如果limit为0，会抛出IllegalArgumentException异常。
     *
     * @param <T> 对象的类型参数
     * @param klass 要查询的Class对象
     * @param limit 返回实例的最大数量，-1表示不限制
     * @return 该类的存活实例数组（最多返回limit个）
     * @throws IllegalArgumentException 如果limit为0
     */
    @Override
    public <T> T[] getInstances(Class<T> klass, int limit) {
        // 验证limit参数
        if (limit == 0) {
            throw new IllegalArgumentException("limit can not be 0");
        }
        // 调用本地方法获取实例
        return getInstances0(klass, limit);
    }

    /**
     * 统计某个类的所有存活实例的总内存占用
     *
     * 该方法返回指定类的所有存活对象占用的堆内存总量。
     *
     * @param klass 要统计的Class对象
     * @return 总内存占用（单位：字节）
     */
    @Override
    public long sumInstanceSize(Class<?> klass) {
        return sumInstanceSize0(klass);
    }

    /**
     * 获取单个对象的内存占用大小
     *
     * 该方法返回指定对象在堆内存中的占用大小。
     *
     * @param instance 要计算的对象实例
     * @return 对象的内存占用大小（单位：字节）
     */
    @Override
    public long getInstanceSize(Object instance) {
        return getInstanceSize0(instance);
    }

    /**
     * 统计某个类的所有存活实例数量
     *
     * 该方法返回JVM中特定类的存活对象实例总数。
     *
     * @param klass 要统计的Class对象
     * @return 该类的存活实例总数
     */
    @Override
    public long countInstances(Class<?> klass) {
        return countInstances0(klass);
    }

    /**
     * 获取所有已加载的类
     *
     * 该方法返回JVM中所有已加载的类的Class对象数组。
     *
     * @return 所有已加载的Class对象数组
     */
    @Override
    public Class<?>[] getAllLoadedClasses() {
        // 必须传入Class.class作为类型标记
        return getAllLoadedClasses0(Class.class);
    }

    /**
     * 调用glibc的malloc_trim函数
     *
     * 该方法调用glibc的malloc_trim()函数，将未使用的内存归还给操作系统。
     * 主要用于Linux系统的内存优化。
     *
     * 返回值：
     * - 返回值大于0：表示实际释放了内存
     * - 返回值等于0：表示没有可释放的内存
     * - 返回值小于0：表示操作失败
     *
     * @return malloc_trim的返回值
     */
    @Override
    public int mallocTrim() {
        return mallocTrim0();
    }

    /**
     * 调用glibc的malloc_trim函数（本地方法）
     *
     * @return malloc_trim的返回值
     */
    private static synchronized native int mallocTrim0();

    /**
     * 调用glibc的malloc_stats函数
     *
     * 该方法调用glibc的malloc_stats()函数，
     * 将内存分配统计信息输出到应用程序的stderr。
     *
     * 输出信息包括：
     * - 分配的内存总量
     * - 可用的内存总量
     * - 内存块的数量和大小分布
     *
     * @return 操作是否成功
     */
    @Override
    public boolean mallocStats() {
        return mallocStats0();
    }

    /**
     * 调用glibc的malloc_stats函数（本地方法）
     *
     * @return 操作是否成功
     */
    private static synchronized native boolean mallocStats0();

    /**
     * 分析堆内存占用最大的对象与类
     *
     * 该方法分析堆内存使用情况，返回内存占用最大的对象和类的信息。
     * 分析从GC Root可达的对象开始进行遍历统计。
     *
     * @param classNum 需要展示的类数量（按内存占用排序，取前N个）
     * @param objectNum 需要展示的对象数量（按内存占用排序，取前N个）
     * @return 分析结果文本，包含：
     *         - 类名和内存占用
     *         - 对象信息和内存占用
     *         - 统计摘要
     */
    @Override
    public String heapAnalyze(int classNum, int objectNum) {
        return heapAnalyze0(classNum, objectNum);
    }

    /**
     * 分析指定类实例的引用回溯链
     *
     * 该方法分析特定类的实例，找出内存占用最大的对象，
     * 并追踪它们到GC Root的完整引用链。
     *
     * @param klass 目标类
     * @param objectNum 需要展示的对象数量（按内存占用排序）
     * @param backtraceNum 回溯层数：
     *        - -1：一直回溯到GC Root
     *        - 0：不输出引用链
     *        - N：回溯N层
     * @return 分析结果文本，包含：
     *         - 对象信息和内存占用
     *         - 引用链信息
     *         - 引用类型（强引用/软引用/弱引用/虚引用）
     */
    @Override
    public String referenceAnalyze(Class<?> klass, int objectNum, int backtraceNum) {
        return referenceAnalyze0(klass, objectNum, backtraceNum);
    }
}
