package com.taobao.arthas.core.command.model;

/**
 * 字段视图对象（Value Object）类
 *
 * <p>该类用于封装Java字段的详细信息，包括字段名、类型、修饰符、注解、值以及是否为静态字段等属性。
 * 主要用于在命令执行结果中展示字段的完整信息。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>封装字段的基本信息：名称、类型、修饰符</li>
 *   <li>存储字段上的注解信息</li>
 *   <li>保存字段的当前值（以ObjectVO形式）</li>
 *   <li>标识字段是否为静态字段</li>
 * </ul>
 *
 * @author gongdewei 2020/4/8
 */
public class FieldVO {
    /**
     * 字段名称
     * 表示Java类中的字段名
     */
    private String name;

    /**
     * 字段类型
     * 表示字段的数据类型，如"int"、"String"、"List"等
     */
    private String type;

    /**
     * 字段修饰符
     * 表示字段的访问修饰符，如"public"、"private"、"protected"等
     */
    private String modifier;

    /**
     * 字段注解数组
     * 存储字段上声明的所有注解的完整名称
     */
    private String[] annotations;

    /**
     * 字段值对象
     * 以ObjectVO形式存储字段的当前值，支持复杂对象的展示
     */
    private ObjectVO value;

    /**
     * 是否为静态字段
     * 标识该字段是否被static修饰符修饰
     */
    private boolean isStatic;

    /**
     * 获取字段名称
     *
     * @return 字段名称字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 设置字段名称
     *
     * @param name 要设置的字段名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取字段类型
     *
     * @return 字段类型字符串，如"int"、"String"等
     */
    public String getType() {
        return type;
    }

    /**
     * 设置字段类型
     *
     * @param type 要设置的字段类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取字段修饰符
     *
     * @return 字段修饰符字符串，如"public"、"private"等
     */
    public String getModifier() {
        return modifier;
    }

    /**
     * 设置字段修饰符
     *
     * @param modifier 要设置的字段修饰符
     */
    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    /**
     * 获取字段值对象
     *
     * @return 字段的值，以ObjectVO形式返回，可能为null
     */
    public ObjectVO getValue() {
        return value;
    }

    /**
     * 设置字段值对象
     *
     * @param value 要设置的字段值对象
     */
    public void setValue(ObjectVO value) {
        this.value = value;
    }

    /**
     * 获取字段注解数组
     *
     * @return 字段上声明的所有注解名称数组，可能为null
     */
    public String[] getAnnotations() {
        return annotations;
    }

    /**
     * 设置字段注解数组
     *
     * @param annotations 要设置的注解名称数组
     */
    public void setAnnotations(String[] annotations) {
        this.annotations = annotations;
    }

    /**
     * 判断字段是否为静态字段
     *
     * @return 如果字段是静态的返回true，否则返回false
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * 设置字段是否为静态字段
     *
     * @param aStatic 如果字段是静态的传入true，否则传入false
     */
    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

}
