package com.taobao.arthas.core.command.model;

/**
 * MBean属性值对象类
 *
 * 该类用于封装JMX MBean（管理Bean）的单个属性信息，
 * 包括属性名、属性值以及可能的错误信息
 *
 * @author gongdewei 2020/4/26
 */
public class MBeanAttributeVO {
    /**
     * 属性名称
     * MBean属性的标识符
     */
    private String name;

    /**
     * 属性值
     * MBean属性的实际值，可以是任意类型
     */
    private Object value;

    /**
     * 错误信息
     * 当获取属性值失败时，存储错误描述信息
     * 如果属性获取成功，该字段为null
     */
    private String error;

    /**
     * 带参数的构造函数（无错误）
     * 用于创建成功获取属性的值对象
     *
     * @param name 属性名称
     * @param value 属性值
     */
    public MBeanAttributeVO(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 带参数的构造函数（包含错误信息）
     * 用于创建获取属性失败时的值对象
     *
     * @param name 属性名称
     * @param value 属性值（可能为null或部分值）
     * @param error 错误信息描述
     */
    public MBeanAttributeVO(String name, Object value, String error) {
        this.name = name;
        this.value = value;
        this.error = error;
    }

    /**
     * 获取属性名称
     *
     * @return 属性名称字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 设置属性名称
     *
     * @param name 要设置的属性名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取属性值
     *
     * @return 属性值对象，类型取决于具体的MBean属性定义
     */
    public Object getValue() {
        return value;
    }

    /**
     * 设置属性值
     *
     * @param value 要设置的属性值
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息字符串，如果没有错误则返回null
     */
    public String getError() {
        return error;
    }

    /**
     * 设置错误信息
     *
     * @param error 要设置的错误信息
     */
    public void setError(String error) {
        this.error = error;
    }
}
