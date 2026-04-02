package com.taobao.arthas.core.command.model;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘命令模型
 * <p>
 * 该类用于封装dashboard命令的执行结果，dashboard命令是Arthas中用于展示Java应用程序整体运行状态的综合性命令。
 * 它聚合了多个维度的系统信息，包括线程状态、内存使用情况、垃圾回收信息、运行时信息以及Tomcat服务器信息等，
 * 为用户提供一个全面的应用健康状态监控视图。该模型继承自ResultModel，可以与Arthas的结果展示框架无缝集成。
 * </p>
 *
 * @author gongdewei 2020/4/22
 */
public class DashboardModel extends ResultModel {

    /**
     * 线程信息列表
     * <p>
     * 存储当前JVM中所有活动线程的状态信息，包括线程ID、名称、状态、CPU使用率等。
     * 用于展示线程池的工作情况和潜在的线程问题。
     * </p>
     */
    private List<ThreadVO> threads;

    /**
     * 内存信息映射
     * <p>
     * 存储JVM各个内存区域的详细使用情况，Key为内存区域名称（如heap、non-heap等），
     * Value为该内存区域下的内存条目列表（包括eden、survivor、old等代区的详细信息）。
     * 用于监控内存使用趋势和发现潜在的内存泄漏问题。
     * </p>
     */
    private Map<String, List<MemoryEntryVO>> memoryInfo;

    /**
     * 垃圾回收信息列表
     * <p>
     * 存储JVM中各个垃圾收集器的运行状态和统计信息，包括GC次数、GC时间、收集器名称等。
     * 用于分析GC性能和调优JVM参数。
     * </p>
     */
    private List<GcInfoVO> gcInfos;

    /**
     * 运行时信息
     * <p>
     * 存储JVM和操作系统的运行时信息，包括JVM版本、启动时间、运行时长、系统属性等。
     * 用于了解应用程序的运行环境和基本配置。
     * </p>
     */
    private RuntimeInfoVO runtimeInfo;

    /**
     * Tomcat信息
     * <p>
     * 存储Tomcat服务器的相关信息，包括连接器状态、线程池配置、请求处理统计等。
     * 仅在应用程序运行在Tomcat容器中时才有值，用于监控Tomcat服务器的健康状态。
     * </p>
     */
    private TomcatInfoVO tomcatInfo;

    /**
     * 获取模型类型
     * <p>
     * 返回此结果模型的类型标识符，用于在前端或客户端识别和区分不同类型的命令结果。
     * dashboard命令的固定返回值为"dashboard"。
     * </p>
     *
     * @return 模型类型标识符，固定返回"dashboard"
     */
    @Override
    public String getType() {
        // 返回dashboard命令的类型标识符
        return "dashboard";
    }

    /**
     * 获取线程信息列表
     * <p>
     * 返回当前JVM中所有活动线程的状态信息，可以用于展示线程池的工作情况、
     * 检测死锁、分析线程竞争等问题。
     * </p>
     *
     * @return 线程信息列表，包含所有活动线程的详细状态
     */
    public List<ThreadVO> getThreads() {
        return threads;
    }

    /**
     * 设置线程信息列表
     * <p>
     * 设置当前JVM中所有活动线程的状态信息。通常在执行dashboard命令时，
     * 会收集JVM中的线程信息并设置到此属性中。
     * </p>
     *
     * @param threads 线程信息列表，包含所有活动线程的详细状态
     */
    public void setThreads(List<ThreadVO> threads) {
        this.threads = threads;
    }

    /**
     * 获取内存信息映射
     * <p>
     * 返回JVM各个内存区域的详细使用情况，包括堆内存和非堆内存的各个分区的使用情况。
     * 可以用于监控内存使用趋势、发现内存泄漏、评估内存分配是否合理。
     * </p>
     *
     * @return 内存信息映射，Key为内存区域名称，Value为该区域下的内存条目列表
     */
    public Map<String, List<MemoryEntryVO>> getMemoryInfo() {
        return memoryInfo;
    }

    /**
     * 设置内存信息映射
     * <p>
     * 设置JVM各个内存区域的详细使用情况。通常在执行dashboard命令时，
     * 会从MemoryMXBean等MBean中收集内存信息并设置到此属性中。
     * </p>
     *
     * @param memoryInfo 内存信息映射，Key为内存区域名称，Value为该区域下的内存条目列表
     */
    public void setMemoryInfo(Map<String, List<MemoryEntryVO>> memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    /**
     * 获取垃圾回收信息列表
     * <p>
     * 返回JVM中各个垃圾收集器的运行状态和统计信息，包括GC次数、GC时间等。
     * 可以用于分析GC频率、评估GC性能、指导JVM参数调优。
     * </p>
     *
     * @return 垃圾回收信息列表，包含所有垃圾收集器的运行统计
     */
    public List<GcInfoVO> getGcInfos() {
        return gcInfos;
    }

    /**
     * 设置垃圾回收信息列表
     * <p>
     * 设置JVM中各个垃圾收集器的运行状态和统计信息。通常在执行dashboard命令时，
     * 会从GarbageCollectorMXBean等MBean中收集GC信息并设置到此属性中。
     * </p>
     *
     * @param gcInfos 垃圾回收信息列表，包含所有垃圾收集器的运行统计
     */
    public void setGcInfos(List<GcInfoVO> gcInfos) {
        this.gcInfos = gcInfos;
    }

    /**
     * 获取运行时信息
     * <p>
     * 返回JVM和操作系统的运行时信息，包括JVM版本、启动时间、运行时长等。
     * 可以用于了解应用程序的运行环境、排查环境相关问题。
     * </p>
     *
     * @return 运行时信息对象，包含JVM和系统的详细运行时数据
     */
    public RuntimeInfoVO getRuntimeInfo() {
        return runtimeInfo;
    }

    /**
     * 设置运行时信息
     * <p>
     * 设置JVM和操作系统的运行时信息。通常在执行dashboard命令时，
     * 会从RuntimeMXBean等MBean中收集运行时信息并设置到此属性中。
     * </p>
     *
     * @param runtimeInfo 运行时信息对象，包含JVM和系统的详细运行时数据
     */
    public void setRuntimeInfo(RuntimeInfoVO runtimeInfo) {
        this.runtimeInfo = runtimeInfo;
    }

    /**
     * 获取Tomcat信息
     * <p>
     * 返回Tomcat服务器的相关信息，包括连接器状态、线程池配置等。
     * 仅在应用程序运行在Tomcat容器中时才有值，可以用于监控Tomcat服务器的健康状态。
     * </p>
     *
     * @return Tomcat信息对象，如果应用不在Tomcat中运行则返回null
     */
    public TomcatInfoVO getTomcatInfo() {
        return tomcatInfo;
    }

    /**
     * 设置Tomcat信息
     * <p>
     * 设置Tomcat服务器的相关信息。通常在执行dashboard命令时，
     * 如果检测到应用运行在Tomcat容器中，会收集Tomcat相关信息并设置到此属性中。
     * </p>
     *
     * @param tomcatInfo Tomcat信息对象，包含Tomcat服务器的详细状态数据
     */
    public void setTomcatInfo(TomcatInfoVO tomcatInfo) {
        this.tomcatInfo = tomcatInfo;
    }
}
