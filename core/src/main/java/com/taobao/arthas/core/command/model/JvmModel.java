package com.taobao.arthas.core.command.model;

import java.util.*;

/**
 * JVM命令结果模型
 * 用于封装jvm命令执行后返回的JVM相关信息
 * 该模型按分组组织JVM信息，每个分组包含多个信息项(JvmItemVO)
 *
 * @author gongdewei 2020/4/24
 */
public class JvmModel extends ResultModel {

    /**
     * JVM信息映射表
     * 键: 分组名称(String), 值: 该分组下的信息项列表(List<JvmItemVO>)
     * 使用线程安全的LinkedHashMap保证插入顺序和并发访问安全
     */
    private Map<String, List<JvmItemVO>> jvmInfo;

    /**
     * 构造函数
     * 初始化一个线程安全的LinkedHashMap来存储JVM信息
     * 使用Collections.synchronizedMap包装以支持多线程并发访问
     */
    public JvmModel() {
        // 创建一个同步的LinkedHashMap，保持插入顺序同时保证线程安全
        jvmInfo = Collections.synchronizedMap(new LinkedHashMap<String, List<JvmItemVO>>());
    }

    /**
     * 获取结果类型
     * 用于标识该模型对应的命令类型
     *
     * @return 返回"jvm"字符串标识
     */
    @Override
    public String getType() {
        return "jvm";
    }

    /**
     * 添加一个无描述的JVM信息项
     * 向指定分组中添加一个只包含名称和值的信息项
     * 采用链式调用设计，方便连续添加多个信息项
     *
     * @param group 分组名称
     * @param name  信息项名称
     * @param value 信息项值
     * @return 返回当前JvmModel实例，支持链式调用
     */
    public JvmModel addItem(String group, String name, Object value) {
        // 委托给带描述参数的addItem方法，描述传null
        this.addItem(group, name, value, null);
        return this;
    }

    /**
     * 添加一个完整的JVM信息项
     * 向指定分组中添加一个包含名称、值和描述的完整信息项
     * 采用链式调用设计，方便连续添加多个信息项
     *
     * @param group 分组名称
     * @param name  信息项名称
     * @param value 信息项值
     * @param desc  信息项描述，可为null
     * @return 返回当前JvmModel实例，支持链式调用
     */
    public JvmModel  addItem(String group, String name, Object value, String desc) {
        // 获取或创建指定分组的列表，然后添加新的信息项
        this.group(group).add(new JvmItemVO(name, value, desc));
        return this;
    }

    /**
     * 获取或创建指定分组的列表
     * 如果指定分组不存在，则创建一个新的列表并放入Map中
     * 该方法是线程安全的，使用synchronized保证并发访问的正确性
     *
     * @param group 分组名称
     * @return 返回该分组对应的信息项列表
     */
    public List<JvmItemVO> group(String group) {
        synchronized (this) {
            // 尝试从Map中获取该分组的列表
            List<JvmItemVO> list = jvmInfo.get(group);
            // 如果该分组不存在，创建一个新的ArrayList
            if (list == null) {
                list = new ArrayList<JvmItemVO>();
                // 将新创建的列表放入Map中
                jvmInfo.put(group, list);
            }
            return list;
        }
    }

    /**
     * 获取完整的JVM信息映射表
     *
     * @return 返回包含所有分组和信息项的Map对象
     */
    public Map<String, List<JvmItemVO>> getJvmInfo() {
        return jvmInfo;
    }

}
