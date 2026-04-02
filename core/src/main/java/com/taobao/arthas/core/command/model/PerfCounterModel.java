package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * PerfCounter命令的模型类
 * <p>
 * 用于封装perfcounter命令的执行结果，该命令用于查看Java进程的性能计数器信息。
 * 性能计数器是JVM内部用于监控和诊断性能指标的重要工具。
 * </p>
 *
 * @author gongdewei 2020/4/27
 */
public class PerfCounterModel extends ResultModel {

    /**
     * 性能计数器列表
     * <p>
     * 存储所有获取到的性能计数器数据，每个计数器包含名称、单位、变化性和值等信息。
     * </p>
     */
    private List<PerfCounterVO> perfCounters;

    /**
     * 是否显示详细信息
     * <p>
     * true表示显示详细的性能计数器信息，包括单位、变化性等额外字段；
     * false表示只显示基本的名称和值信息。
     * </p>
     */
    private boolean details;

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的PerfCounterModel实例，所有字段使用默认值。
     * </p>
     */
    public PerfCounterModel() {
    }

    /**
     * 全参数构造函数
     * <p>
     * 创建一个包含完整信息的PerfCounterModel实例。
     * </p>
     *
     * @param perfCounters 性能计数器列表，包含所有获取到的计数器数据
     * @param details      是否显示详细信息的标志位
     */
    public PerfCounterModel(List<PerfCounterVO> perfCounters, boolean details) {
        this.perfCounters = perfCounters;
        this.details = details;
    }

    /**
     * 获取模型类型
     * <p>
     * 返回此模型的类型标识符，用于前端识别如何渲染此模型数据。
     * </p>
     *
     * @return 模型类型字符串，固定返回"perfcounter"
     */
    @Override
    public String getType() {
        return "perfcounter";
    }

    /**
     * 获取性能计数器列表
     *
     * @return 性能计数器列表，可能为null
     */
    public List<PerfCounterVO> getPerfCounters() {
        return perfCounters;
    }

    /**
     * 设置性能计数器列表
     *
     * @param perfCounters 要设置的性能计数器列表
     */
    public void setPerfCounters(List<PerfCounterVO> perfCounters) {
        this.perfCounters = perfCounters;
    }

    /**
     * 判断是否显示详细信息
     *
     * @return true表示显示详细信息，false表示只显示基本信息
     */
    public boolean isDetails() {
        return details;
    }

    /**
     * 设置是否显示详细信息
     *
     * @param details true表示显示详细信息，false表示只显示基本信息
     */
    public void setDetails(boolean details) {
        this.details = details;
    }
}
