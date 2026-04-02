package com.taobao.arthas.core.command.model;

/**
 * 对象值对象（包装类）
 * <pre>
 * 包装一层，解决 JSON 输出问题
 * 问题参考：https://github.com/alibaba/arthas/issues/2261
 *
 * 该类用于包装任意对象，并提供扩展控制功能，
 * 主要用于解决 JSON 序列化时的特殊需求。
 * </pre>
 *
 * @author hengyunabc 2022-08-24
 */
public class ObjectVO {

    /**
     * 被包装的对象
     * 可以是任意类型的 Java 对象
     */
    private Object object;

    /**
     * 扩展级别
     * 控制对象在输出时的详细程度或展开层级
     * null 表示未设置，0 表示不展开，正整数表示展开的层级数
     */
    private Integer expand;

    /**
     * 构造函数
     *
     * @param object 被包装的对象
     * @param expand 扩展级别
     */
    public ObjectVO(Object object, Integer expand) {
        this.object = object;
        this.expand = expand;
    }

    /**
     * 批量包装对象数组
     * 将对象数组中的每个对象都包装成 ObjectVO
     *
     * @param objects 要包装的对象数组
     * @param expand  扩展级别，应用于所有包装后的对象
     * @return ObjectVO 数组，长度与输入数组相同
     */
    public static ObjectVO[] array(Object[] objects, Integer expand) {
        // 创建结果数组，长度与输入数组相同
        ObjectVO[] result = new ObjectVO[objects.length];
        // 遍历输入数组，逐个包装对象
        for (int i = 0; i < objects.length; ++i) {
            result[i] = new ObjectVO(objects[i], expand);
        }
        return result;
    }

    /**
     * 获取扩展级别，如果未设置则返回默认值 1
     *
     * @return 扩展级别，如果未设置则返回 1
     */
    public int expandOrDefault() {
        // 如果扩展级别不为空，则返回设置的值
        if (expand != null) {
            return expand;
        }
        // 默认返回 1
        return 1;
    }

    /**
     * 判断是否需要展开对象
     *
     * @return true 表示需要展开（expand 不为 null 且大于 0），false 表示不需要展开
     */
    public boolean needExpand() {
        // 只有当 expand 不为 null 且值大于 0 时才需要展开
        return null != expand && expand > 0;
    }

    /**
     * 获取被包装的对象
     *
     * @return 被包装的对象
     */
    public Object getObject() {
        return object;
    }

    /**
     * 设置被包装的对象
     *
     * @param object 要包装的对象
     */
    public void setObject(Object object) {
        this.object = object;
    }

    /**
     * 获取扩展级别
     *
     * @return 扩展级别，可能为 null
     */
    public Integer getExpand() {
        return expand;
    }

    /**
     * 设置扩展级别
     *
     * @param expand 扩展级别
     */
    public void setExpand(Integer expand) {
        this.expand = expand;
    }
}
