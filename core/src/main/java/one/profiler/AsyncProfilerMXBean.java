/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package one.profiler;

/**
 * AsyncProfiler的JMX服务器接口
 * 用于通过JMX方式管理和控制AsyncProfiler性能分析器
 * 如何注册AsyncProfiler MBean:
 *
 * <pre>{@code
 *     ManagementFactory.getPlatformMBeanServer().registerMBean(
 *             AsyncProfiler.getInstance(),
 *             new ObjectName("one.profiler:type=AsyncProfiler")
 *     );
 * }</pre>
 */
public interface AsyncProfilerMXBean {
    /**
     * MBean对象名称，用于在JMX注册表中唯一标识此MBean
     */
    String OBJECT_NAME = "one.profiler:type=AsyncProfiler";

    /**
     * 启动性能分析器
     *
     * @param event 要监听的事件类型（如cpu、alloc、lock等）
     * @param interval 采样间隔（纳秒）
     * @throws IllegalStateException 如果分析器已经启动
     */
    void start(String event, long interval) throws IllegalStateException;

    /**
     * 恢复性能分析器
     * 用于在暂停后继续性能分析
     *
     * @param event 要监听的事件类型
     * @param interval 采样间隔（纳秒）
     * @throws IllegalStateException 如果分析器已经处于运行状态
     */
    void resume(String event, long interval) throws IllegalStateException;

    /**
     * 停止性能分析器
     *
     * @throws IllegalStateException 如果分析器未启动
     */
    void stop() throws IllegalStateException;

    /**
     * 获取已采集的样本数量
     *
     * @return 采集的样本总数
     */
    long getSamples();

    /**
     * 获取AsyncProfiler的版本信息
     *
     * @return 版本字符串
     */
    String getVersion();

    /**
     * 执行AsyncProfiler命令
     * 用于通过命令字符串方式控制分析器
     *
     * @param command 命令字符串（如start,stop,dump等）
     * @return 命令执行结果
     * @throws IllegalArgumentException 如果命令参数无效
     * @throws IllegalStateException 如果分析器状态不允许执行该命令
     * @throws java.io.IOException 如果发生I/O错误
     */
    String execute(String command) throws IllegalArgumentException, IllegalStateException, java.io.IOException;

    /**
     * 导出折叠式堆栈跟踪格式的性能分析数据
     * 折叠格式是一种简洁的堆栈表示方法，每行表示一个调用栈，用分号分隔
     *
     * @param counter 计数器类型，指定使用样本数还是总时间作为度量
     * @return 折叠格式的性能分析字符串
     */
    String dumpCollapsed(Counter counter);

    /**
     * 导出跟踪格式的性能分析数据
     * 跟踪格式显示完整的调用树，包括每个方法的调用次数和样本数
     *
     * @param maxTraces 最大跟踪栈数量，限制输出的调用栈数量
     * @return 跟踪格式的性能分析字符串
     */
    String dumpTraces(int maxTraces);

    /**
     * 导出扁平格式的性能分析数据
     * 扁平格式按方法汇总统计，不显示调用关系
     *
     * @param maxMethods 最大方法数量，限制输出的方法数量
     * @return 扁平格式的性能分析字符串
     */
    String dumpFlat(int maxMethods);

    /**
     * 导出OTLP（OpenTelemetry Protocol）格式的性能分析数据
     * OTLP是OpenTelemetry的协议格式，可用于与其他监控系统集成
     *
     * @return OTLP格式的二进制数据
     */
    byte[] dumpOtlp();
}