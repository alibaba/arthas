package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.JvmModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.management.*;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JVM信息查看命令
 *
 * 该命令用于显示目标JVM的各种信息，包括运行时、类加载、编译、
 * 垃圾回收、内存管理、操作系统、线程等多个方面的详细信息。
 *
 * @author vlinux on 15/6/6.
 */
@Name("jvm")
@Summary("Display the target JVM information")
@Description(Constants.WIKI + Constants.WIKI_HOME + "jvm")
public class JvmCommand extends AnnotatedCommand {

    // 运行时MBean，提供JVM运行时信息
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    // 类加载MBean，提供类加载统计信息
    private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

    // 编译MBean，提供JIT编译信息
    private final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();

    // 垃圾收集器MBean集合，提供GC信息
    private final Collection<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

    // 内存管理器MBean集合
    private final Collection<MemoryManagerMXBean> memoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans();

    // 内存MBean，提供堆内存和非堆内存信息
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    // 内存池MBean集合（已注释，当前未使用）
    //private final Collection<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

    // 操作系统MBean，提供OS相关信息
    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

    // 线程MBean，提供线程相关信息
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 执行JVM命令
     *
     * 该方法收集JVM的各种信息并输出到命令进程。
     * 信息包括：运行时、类加载、编译、垃圾回收、内存管理、内存、操作系统、线程、文件描述符等。
     *
     * @param process 命令进程对象
     */
    @Override
    public void process(CommandProcess process) {

        // 创建JVM模型对象，用于存储和传递JVM信息
        JvmModel jvmModel = new JvmModel();

        // 添加运行时信息（如JVM启动时间、输入参数等）
        addRuntimeInfo(jvmModel);

        // 添加类加载信息（如已加载类数量、总加载类数量等）
        addClassLoading(jvmModel);

        // 添加编译信息（如编译器名称、总编译时间等）
        addCompilation(jvmModel);

        // 如果存在垃圾收集器，添加GC信息
        if (!garbageCollectorMXBeans.isEmpty()) {
            addGarbageCollectors(jvmModel);
        }

        // 如果存在内存管理器，添加内存管理器信息
        if (!memoryManagerMXBeans.isEmpty()) {
            addMemoryManagers(jvmModel);
        }

        // 添加内存信息（堆内存和非堆内存使用情况）
        addMemory(jvmModel);

        // 添加操作系统信息（如OS名称、架构、处理器数量等）
        addOperatingSystem(jvmModel);

        // 添加线程信息（如线程数量、死锁线程数等）
        addThread(jvmModel);

        // 添加文件描述符信息（最大和打开的文件描述符数量）
        addFileDescriptor(jvmModel);

        // 将JVM模型添加到命令进程结果中
        process.appendResult(jvmModel);
        // 标记命令执行完成
        process.end();
    }

    /**
     * 添加文件描述符信息
     *
     * 文件描述符是Unix/Linux系统中用于访问文件或其他输入/输出资源（如管道或网络套接字）的抽象指示符。
     * 该方法获取并添加最大文件描述符数量和当前打开的文件描述符数量。
     *
     * @param jvmModel JVM模型对象
     */
    private void addFileDescriptor(JvmModel jvmModel) {
        String group = "FILE-DESCRIPTOR";
        // 添加最大文件描述符数量
        jvmModel.addItem(group,"MAX-FILE-DESCRIPTOR-COUNT", invokeFileDescriptor(operatingSystemMXBean, "getMaxFileDescriptorCount"))
                // 添加当前打开的文件描述符数量
                .addItem(group,"OPEN-FILE-DESCRIPTOR-COUNT", invokeFileDescriptor(operatingSystemMXBean, "getOpenFileDescriptorCount"));
    }

    /**
     * 通过反射调用文件描述符相关方法
     *
     * 由于getMaxFileDescriptorCount和getOpenFileDescriptorCount方法
     * 在OperatingSystemMXBean接口中不存在（只在UnixOperatingSystem中存在），
     * 所以需要通过反射来调用这些方法。
     *
     * @param os 操作系统MBean对象
     * @param name 要调用的方法名
     * @return 方法调用的返回值，如果调用失败则返回-1
     */
    private long invokeFileDescriptor(OperatingSystemMXBean os, String name) {
        try {
            // 获取指定名称的方法
            final Method method = os.getClass().getDeclaredMethod(name);
            // 设置方法可访问（可能需要覆盖访问控制）
            method.setAccessible(true);
            // 调用方法并返回结果
            return (Long) method.invoke(os);
        } catch (Exception e) {
            // 如果调用失败（如在非Unix系统上），返回-1表示不支持
            return -1;
        }
    }

    /**
     * 添加运行时信息
     *
     * 运行时信息包括JVM的启动时间、规范版本、VM名称和版本、
     * 输入参数、类路径、引导类路径、库路径等。
     *
     * @param jvmModel JVM模型对象
     */
    private void addRuntimeInfo(JvmModel jvmModel) {
        // 获取引导类路径
        String bootClassPath = "";
        try {
            bootClassPath = runtimeMXBean.getBootClassPath();
        } catch (Exception e) {
            // 在JDK9及以上版本会抛出UnsupportedOperationException，忽略此异常
        }
        String group = "RUNTIME";
        // 添加机器名称（通常是JVM进程的标识符）
        jvmModel.addItem(group,"MACHINE-NAME", runtimeMXBean.getName());
        // 添加JVM启动时间（格式化为可读的日期时间字符串）
        jvmModel.addItem(group, "JVM-START-TIME", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(runtimeMXBean.getStartTime())));
        // 添加管理规范版本
        jvmModel.addItem(group, "MANAGEMENT-SPEC-VERSION", runtimeMXBean.getManagementSpecVersion());
        // 添加规范名称（通常是Java虚拟机规范）
        jvmModel.addItem(group, "SPEC-NAME", runtimeMXBean.getSpecName());
        // 添加规范供应商（通常是Oracle Corporation）
        jvmModel.addItem(group, "SPEC-VENDOR", runtimeMXBean.getSpecVendor());
        // 添加规范版本
        jvmModel.addItem(group, "SPEC-VERSION", runtimeMXBean.getSpecVersion());
        // 添加VM名称（如Java HotSpot(TM) 64-Bit Server VM）
        jvmModel.addItem(group, "VM-NAME", runtimeMXBean.getVmName());
        // 添加VM供应商
        jvmModel.addItem(group, "VM-VENDOR", runtimeMXBean.getVmVendor());
        // 添加VM版本
        jvmModel.addItem(group, "VM-VERSION", runtimeMXBean.getVmVersion());
        // 添加输入参数（JVM启动时传递的参数，如-Xmx、-Xms等）
        jvmModel.addItem(group, "INPUT-ARGUMENTS", runtimeMXBean.getInputArguments());
        // 添加类路径（应用程序类路径）
        jvmModel.addItem(group, "CLASS-PATH", runtimeMXBean.getClassPath());
        // 添加引导类路径（JVM核心类路径）
        jvmModel.addItem(group, "BOOT-CLASS-PATH", bootClassPath);
        // 添加库路径（本地库搜索路径）
        jvmModel.addItem(group, "LIBRARY-PATH", runtimeMXBean.getLibraryPath());
    }

    /**
     * 添加类加载信息
     *
     * 类加载信息包括当前已加载的类数量、总共加载的类数量、
     * 已卸载的类数量以及是否处于详细模式。
     *
     * @param jvmModel JVM模型对象
     */
    private void addClassLoading(JvmModel jvmModel) {
        String group = "CLASS-LOADING";
        // 添加当前已加载的类数量（包括已加载但未卸载的类）
        jvmModel.addItem(group, "LOADED-CLASS-COUNT", classLoadingMXBean.getLoadedClassCount());
        // 添加自JVM启动以来总共加载的类数量
        jvmModel.addItem(group, "TOTAL-LOADED-CLASS-COUNT", classLoadingMXBean.getTotalLoadedClassCount());
        // 添加已卸载的类数量
        jvmModel.addItem(group, "UNLOADED-CLASS-COUNT", classLoadingMXBean.getUnloadedClassCount());
        // 添加是否处于详细模式（verbose模式会输出类加载信息）
        jvmModel.addItem(group, "IS-VERBOSE", classLoadingMXBean.isVerbose());
    }

    /**
     * 添加编译信息
     *
     * 编译信息包括JIT编译器的名称和总编译时间（如果支持编译时间监控）。
     *
     * @param jvmModel JVM模型对象
     */
    private void addCompilation(JvmModel jvmModel) {
        // 如果编译MBean为null（如使用解释模式的JVM），直接返回
        if (compilationMXBean == null) {
            return;
        }
        String group = "COMPILATION";
        // 添加编译器名称（如HotSpot 64-Bit Tiered Compilers）
        jvmModel.addItem(group, "NAME", compilationMXBean.getName());
        // 如果支持编译时间监控，添加总编译时间
        if (compilationMXBean.isCompilationTimeMonitoringSupported()) {
            jvmModel.addItem(group, "TOTAL-COMPILE-TIME", compilationMXBean.getTotalCompilationTime(), "time (ms)");
        }
    }

    /**
     * 添加垃圾收集器信息
     *
     * 垃圾收集器信息包括每个GC的名称、回收次数和回收时间。
     * JVM可能同时运行多个垃圾收集器（如新生代GC和老年代GC）。
     *
     * @param jvmModel JVM模型对象
     */
    private void addGarbageCollectors(JvmModel jvmModel) {
        String group = "GARBAGE-COLLECTORS";
        // 遍历所有垃圾收集器MBean
        for (GarbageCollectorMXBean gcMXBean : garbageCollectorMXBeans) {
            // 创建GC信息映射表
            Map<String, Object> gcInfo = new LinkedHashMap<String, Object>();
            // 添加GC名称（如G1 Young Generation、G1 Old Generation）
            gcInfo.put("name", gcMXBean.getName());
            // 添加GC次数
            gcInfo.put("collectionCount", gcMXBean.getCollectionCount());
            // 添加GC时间（毫秒）
            gcInfo.put("collectionTime", gcMXBean.getCollectionTime());

            // 将GC信息添加到模型中，并注明单位为"次数/时间(毫秒)"
            jvmModel.addItem(group, gcMXBean.getName(), gcInfo, "count/time (ms)");
        }
    }

    /**
     * 添加内存管理器信息
     *
     * 内存管理器信息包括每个内存管理器的名称和管理的内存池。
     * 内存管理器负责管理一个或多个内存池。
     *
     * @param jvmModel JVM模型对象
     */
    private void addMemoryManagers(JvmModel jvmModel) {
        String group = "MEMORY-MANAGERS";
        // 遍历所有内存管理器MBean
        for (final MemoryManagerMXBean memoryManagerMXBean : memoryManagerMXBeans) {
            // 检查内存管理器是否有效
            if (memoryManagerMXBean.isValid()) {
                // 获取内存管理器名称
                final String name = memoryManagerMXBean.isValid()
                        ? memoryManagerMXBean.getName()
                        : memoryManagerMXBean.getName() + "(Invalid)";
                // 添加内存管理器及其管理的内存池名称
                jvmModel.addItem(group, name, memoryManagerMXBean.getMemoryPoolNames());
            }
        }
    }

    /**
     * 添加内存信息
     *
     * 内存信息包括堆内存和非堆内存的使用情况，
     * 以及等待终结的对象数量。
     *
     * @param jvmModel JVM模型对象
     */
    private void addMemory(JvmModel jvmModel) {
        String group = "MEMORY";
        // 获取堆内存使用情况
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        // 构建堆内存信息映射表
        Map<String, Object> heapMemoryInfo = getMemoryUsageInfo("heap", heapMemoryUsage);
        // 添加堆内存使用信息，单位为字节
        jvmModel.addItem(group, "HEAP-MEMORY-USAGE", heapMemoryInfo, "memory in bytes");

        // 获取非堆内存使用情况（包括方法区、代码缓存等）
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        // 构建非堆内存信息映射表
        Map<String, Object> nonheapMemoryInfo = getMemoryUsageInfo("nonheap", nonHeapMemoryUsage);
        // 添加非堆内存使用信息，单位为字节
        jvmModel.addItem(group,"NO-HEAP-MEMORY-USAGE", nonheapMemoryInfo, "memory in bytes");

        // 添加等待终结的对象数量（这些对象的finalize方法尚未被执行）
        jvmModel.addItem(group,"PENDING-FINALIZE-COUNT", memoryMXBean.getObjectPendingFinalizationCount());
    }

    /**
     * 构建内存使用信息映射表
     *
     * 该方法将MemoryUsage对象转换为Map格式，
     * 包含内存的初始化大小、已使用大小、已提交大小和最大大小。
     *
     * @param name 内存名称（如"heap"或"nonheap"）
     * @param heapMemoryUsage 内存使用情况对象
     * @return 包含内存使用信息的映射表
     */
    private Map<String, Object> getMemoryUsageInfo(String name, MemoryUsage heapMemoryUsage) {
        Map<String, Object> memoryInfo = new LinkedHashMap<String, Object>();
        // 添加内存名称
        memoryInfo.put("name", name);
        // 添加初始化大小（JVM启动时请求的初始内存量）
        memoryInfo.put("init", heapMemoryUsage.getInit());
        // 添加已使用大小（当前实际使用的内存量）
        memoryInfo.put("used", heapMemoryUsage.getUsed());
        // 添加已提交大小（保证可用的内存量）
        memoryInfo.put("committed", heapMemoryUsage.getCommitted());
        // 添加最大大小（可以使用的最大内存量）
        memoryInfo.put("max", heapMemoryUsage.getMax());
        return memoryInfo;
    }

    /**
     * 添加操作系统信息
     *
     * 操作系统信息包括OS名称、架构、处理器数量、系统平均负载和OS版本。
     *
     * @param jvmModel JVM模型对象
     */
    private void addOperatingSystem(JvmModel jvmModel) {
        String group = "OPERATING-SYSTEM";
        // 添加操作系统名称（如Linux、Mac OS X等）
        jvmModel.addItem(group,"OS", operatingSystemMXBean.getName())
                // 添加系统架构（如x86_64、aarch64等）
                .addItem(group,"ARCH", operatingSystemMXBean.getArch())
                // 添加可用处理器数量
                .addItem(group,"PROCESSORS-COUNT", operatingSystemMXBean.getAvailableProcessors())
                // 添加系统平均负载（最近一分钟的系统负载平均值）
                .addItem(group,"LOAD-AVERAGE", operatingSystemMXBean.getSystemLoadAverage())
                // 添加操作系统版本
                .addItem(group,"VERSION", operatingSystemMXBean.getVersion());
    }

    /**
     * 添加线程信息
     *
     * 线程信息包括线程总数、守护线程数、峰值线程数、
     * 已启动线程总数和死锁线程数量。
     *
     * @param jvmModel JVM模型对象
     */
    private void addThread(JvmModel jvmModel) {
        String group = "THREAD";
        // 添加当前活动线程数量
        jvmModel.addItem(group, "COUNT", threadMXBean.getThreadCount())
                // 添加当前守护线程数量
                .addItem(group, "DAEMON-COUNT", threadMXBean.getDaemonThreadCount())
                // 添加自JVM启动以来的峰值线程数量
                .addItem(group, "PEAK-COUNT", threadMXBean.getPeakThreadCount())
                // 添加自JVM启动以来创建的线程总数（包括已终止的线程）
                .addItem(group, "STARTED-COUNT", threadMXBean.getTotalStartedThreadCount())
                // 添加当前死锁线程数量
                .addItem(group, "DEADLOCK-COUNT",getDeadlockedThreadsCount(threadMXBean));
    }

    /**
     * 获取死锁线程数量
     *
     * 该方法检测当前处于死锁状态的线程数量。
     * 死锁是指两个或多个线程互相等待对方持有的锁，导致所有线程都无法继续执行。
     *
     * @param threads 线程MBean对象
     * @return 死锁线程数量，如果没有死锁则返回0
     */
    private int getDeadlockedThreadsCount(ThreadMXBean threads) {
        // 查找所有处于死锁状态的线程ID
        final long[] ids = threads.findDeadlockedThreads();
        if (ids == null) {
            // 如果返回null，表示没有检测到死锁
            return 0;
        } else {
            // 返回死锁线程的数量
            return ids.length;
        }
    }
}
