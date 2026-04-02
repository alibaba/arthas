package com.taobao.arthas.core.command.model;


/**
 * 性能计数器值对象（Value Object）
 * <p>
 * 用于封装单个性能计数器的完整信息，包括计数器名称、计量单位、
 * 变化性和当前值等属性。性能计数器是JVM提供的用于监控和诊断
 * 应用程序性能的重要工具。
 * </p>
 * <p>
 * 常见的性能计数器包括：
 * <ul>
 *   <li>cpuTime: CPU使用时间</li>
 *   <li>heapUsed: 堆内存使用量</li>
 *   <li>threadCount: 线程数量</li>
 *   <li>gcCount: 垃圾回收次数</li>
 * </ul>
 * </p>
 *
 * @author gongdewei 2020/4/27
 */
public class PerfCounterVO {

    /**
     * 性能计数器名称
     * <p>
     * 标识计数器的唯一名称，例如"java.lang:type=Memory.HeapMemoryUsage.used"。
     * 名称通常遵循JMX（Java Management Extensions）的命名规范。
     * </p>
     */
    private String name;

    /**
     * 计量单位
     * <p>
     * 计数器值的计量单位，例如：
     * <ul>
     *   <li>bytes: 字节，用于内存相关计数器</li>
     *   <li>ms: 毫秒，用于时间相关计数器</li>
     *   <li>count: 次数，用于事件计数器</li>
     *   <li>ops: 操作数，用于吞吐量计数器</li>
     * </ul>
     * </p>
     */
    private String units;

    /**
     * 变化性
     * <p>
     * 描述计数器值的变化特性，常见值包括：
     * <ul>
     *   <li>Constant: 常量，值不会改变（如JVM版本）</li>
     *   <li>Monotonic: 单调递增，值只增不减（如CPU时间）</li>
     *   <li>Volatile: 易变，值可能任意变化（如堆内存使用量）</li>
     * </ul>
     * </p>
     */
    private String variability;

    /**
     * 计数器值
     * <p>
     * 性能计数器的当前值，可以是各种类型：
     * <ul>
     *   <li>Long: 整数值（如内存字节数、线程数）</li>
     *   <li>Double: 浮点数值（如百分比、比率）</li>
     *   <li>String: 字符串值（如状态描述）</li>
     * </ul>
     * </p>
     */
    private Object value;

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的PerfCounterVO实例，所有字段使用默认值（null）。
     * </p>
     */
    public PerfCounterVO() {
    }

    /**
     * 简化构造函数
     * <p>
     * 创建一个只包含名称和值的PerfCounterVO实例，
     * 适用于只需要基本信息的场景。
     * </p>
     *
     * @param name  性能计数器名称
     * @param value 性能计数器值
     */
    public PerfCounterVO(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 完整构造函数
     * <p>
     * 创建一个包含所有信息的PerfCounterVO实例，
     * 适用于需要完整计数器元数据的场景。
     * </p>
     *
     * @param name         性能计数器名称
     * @param units        计量单位
     * @param variability  变化性描述
     * @param value        性能计数器值
     */
    public PerfCounterVO(String name, String units, String variability, Object value) {
        this.name = name;
        this.units = units;
        this.variability = variability;
        this.value = value;
    }

    /**
     * 设置性能计数器名称
     *
     * @param name 性能计数器名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 设置计量单位
     *
     * @param units 计量单位
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * 设置变化性描述
     *
     * @param variability 变化性描述
     */
    public void setVariability(String variability) {
        this.variability = variability;
    }

    /**
     * 设置计数器值
     *
     * @param value 计数器值
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * 获取性能计数器名称
     *
     * @return 性能计数器名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取计量单位
     *
     * @return 计量单位
     */
    public String getUnits() {
        return units;
    }

    /**
     * 获取变化性描述
     *
     * @return 变化性描述
     */
    public String getVariability() {
        return variability;
    }

    /**
     * 获取计数器值
     *
     * @return 计数器值
     */
    public Object getValue() {
        return value;
    }
}
