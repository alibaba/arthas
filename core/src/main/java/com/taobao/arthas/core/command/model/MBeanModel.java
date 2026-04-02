package com.taobao.arthas.core.command.model;

import javax.management.MBeanInfo;
import java.util.List;
import java.util.Map;

/**
 * MBean命令的结果模型类
 *
 * 该类用于封装mbean命令的执行结果，包括MBean名称列表、
 * MBean元数据信息以及MBean属性值信息
 * 继承自ResultModel基类，用于命令执行结果的统一管理和传输
 *
 * @author gongdewei 2020/4/26
 */
public class MBeanModel extends ResultModel {

    /**
     * MBean名称列表
     * 存储匹配到的MBean对象名称列表
     * MBean名称通常采用"domain:type=xxx"的格式
     */
    private List<String> mbeanNames;

    /**
     * MBean元数据映射表
     * 键为MBean名称，值为对应的MBeanInfo对象
     * MBeanInfo包含了MBean的描述信息、操作、属性、构造器等元数据
     */
    private Map<String, MBeanInfo> mbeanMetadata;

    /**
     * MBean属性值映射表
     * 键为MBean名称，值为该MBean的属性列表
     * 每个属性以MBeanAttributeVO对象的形式存储，包含属性名、值和可能的错误信息
     */
    private Map<String, List<MBeanAttributeVO>> mbeanAttribute;

    /**
     * 默认构造函数
     * 创建一个空的MBeanModel实例
     */
    public MBeanModel() {
    }

    /**
     * 带参数的构造函数
     *
     * @param mbeanNames MBean名称列表
     */
    public MBeanModel(List<String> mbeanNames) {
        this.mbeanNames = mbeanNames;
    }

    /**
     * 获取模型类型标识
     * 用于在序列化和反序列化时识别模型类型
     *
     * @return 类型标识字符串，固定返回"mbean"
     */
    @Override
    public String getType() {
        return "mbean";
    }

    /**
     * 获取MBean名称列表
     *
     * @return MBean名称列表
     */
    public List<String> getMbeanNames() {
        return mbeanNames;
    }

    /**
     * 设置MBean名称列表
     *
     * @param mbeanNames 要设置的MBean名称列表
     */
    public void setMbeanNames(List<String> mbeanNames) {
        this.mbeanNames = mbeanNames;
    }

    /**
     * 获取MBean元数据映射表
     *
     * @return MBean元数据映射表，键为MBean名称，值为MBeanInfo对象
     */
    public Map<String, MBeanInfo> getMbeanMetadata() {
        return mbeanMetadata;
    }

    /**
     * 设置MBean元数据映射表
     *
     * @param mbeanMetadata 要设置的MBean元数据映射表
     */
    public void setMbeanMetadata(Map<String, MBeanInfo> mbeanMetadata) {
        this.mbeanMetadata = mbeanMetadata;
    }

    /**
     * 获取MBean属性值映射表
     *
     * @return MBean属性值映射表，键为MBean名称，值为属性列表
     */
    public Map<String, List<MBeanAttributeVO>> getMbeanAttribute() {
        return mbeanAttribute;
    }

    /**
     * 设置MBean属性值映射表
     *
     * @param mbeanAttribute 要设置的MBean属性值映射表
     */
    public void setMbeanAttribute(Map<String, List<MBeanAttributeVO>> mbeanAttribute) {
        this.mbeanAttribute = mbeanAttribute;
    }
}
