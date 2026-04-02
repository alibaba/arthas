package com.taobao.arthas.core.command.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统属性键值对结果模型
 * <p>
 * 该类用于封装获取或修改系统属性命令的执行结果
 * 使用HashMap存储系统属性，提供快速的键值对访问
 * </p>
 *
 * @author gongdewei 2020/4/2
 */
public class SystemPropertyModel extends ResultModel {

    /**
     * 系统属性映射表
     * <p>
     * 使用HashMap存储系统属性键值对，提供高效的存取性能
     * Key为属性名，Value为属性值
     * </p>
     */
    private Map<String, String> props = new HashMap<String, String>();

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的系统属性模型
     * </p>
     */
    public SystemPropertyModel() {
    }

    /**
     * 通过Map构造系统属性模型
     * <p>
     * 将给定的Map中的所有键值对添加到系统属性映射表中
     * </p>
     *
     * @param props 包含系统属性键值对的Map对象
     */
    public SystemPropertyModel(Map props) {
        this.putAll(props);
    }

    /**
     * 通过单个键值对构造系统属性模型
     * <p>
     * 创建包含单个系统属性的模型
     * </p>
     *
     * @param name  属性名
     * @param value 属性值
     */
    public SystemPropertyModel(String name, String value) {
        this.put(name, value);
    }

    /**
     * 获取系统属性映射表
     *
     * @return 包含所有系统属性键值对的Map对象
     */
    public Map<String, String> getProps() {
        return props;
    }

    /**
     * 添加或更新单个系统属性
     * <p>
     * 如果指定的键已存在，则覆盖其值；否则添加新的键值对
     * </p>
     *
     * @param key   属性名
     * @param value 属性值
     * @return 该键之前关联的值，如果没有则返回null
     */
    public String put(String key, String value) {
        return props.put(key, value);
    }

    /**
     * 批量添加系统属性
     * <p>
     * 将给定Map中的所有键值对添加到系统属性映射表中
     * </p>
     *
     * @param m 包含系统属性键值对的Map对象
     */
    public void putAll(Map m) {
        props.putAll(m);
    }

    /**
     * 获取结果类型
     * <p>
     * 返回"sysprop"标识这是一个系统属性命令的结果
     * </p>
     *
     * @return 结果类型字符串"sysprop"
     */
    @Override
    public String getType() {
        return "sysprop";
    }
}
