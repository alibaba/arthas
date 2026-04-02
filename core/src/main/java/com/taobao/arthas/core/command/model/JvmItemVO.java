package com.taobao.arthas.core.command.model;

/**
 * JVM信息项视图对象
 * 用于封装JVM相关信息的单个数据项，采用键值对描述的结构
 * 包含名称(name)、值(value)和描述(desc)三个核心属性
 *
 * @author gongdewei 2020/4/24
 */
public class JvmItemVO {
    /**
     * 信息项名称
     * 用于标识该JVM信息项的名称或键
     */
    private String name;

    /**
     * 信息项的值
     * 存储该JVM信息项的实际值，使用Object类型以支持不同类型的数据
     */
    private Object value;

    /**
     * 信息项的描述
     * 对该JVM信息项进行详细说明或描述
     */
    private String desc;

    /**
     * 全参数构造函数
     * 创建一个包含名称、值和描述的完整JVM信息项
     *
     * @param name  信息项的名称
     * @param value 信息项的值
     * @param desc  信息项的描述
     */
    public JvmItemVO(String name, Object value, String desc) {
        this.name = name;
        this.value = value;
        this.desc = desc;
    }

    /**
     * 简化构造函数
     * 创建一个只包含名称和值的JVM信息项，描述字段为null
     *
     * @param name  信息项的名称
     * @param value 信息项的值
     */
    public JvmItemVO(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 获取信息项名称
     *
     * @return 返回信息项的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置信息项名称
     *
     * @param name 要设置的名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取信息项描述
     *
     * @return 返回信息项的描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 设置信息项描述
     *
     * @param desc 要设置的描述
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * 获取信息项值
     *
     * @return 返回信息项的值
     */
    public Object getValue() {
        return value;
    }

    /**
     * 设置信息项值
     *
     * @param value 要设置的值
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
