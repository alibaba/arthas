package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * GetStatic命令的数据模型
 * 用于封装getstatic命令的执行结果，包括匹配的类、字段信息、类加载器等信息
 *
 * @author gongdewei 2020/4/20
 */
public class GetStaticModel extends ResultModel {

    /**
     * 匹配的类集合
     * 当指定类表达式匹配多个类时，使用此字段存储所有匹配的类
     */
    private Collection<ClassVO> matchedClasses;

    /**
     * 字段名称
     * 获取的静态字段的名称
     */
    private String fieldName;

    /**
     * 字段值对象
     * 使用ObjectVO封装静态字段的值，支持对象展开显示
     */
    private ObjectVO field;

    /**
     * 匹配的类加载器集合
     * 当指定类加载器表达式匹配多个类加载器时，使用此字段存储所有匹配的类加载器
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 指定使用的类加载器的类名
     */
    private String classLoaderClass;

    /**
     * 默认构造函数
     * 创建一个空的GetStaticModel实例
     */
    public GetStaticModel() {
    }

    /**
     * 构造函数 - 用于创建包含字段值的模型
     *
     * @param fieldName 字段名称
     * @param fieldValue 字段的值
     * @param expand 展开层级，用于控制对象的展开深度
     */
    public GetStaticModel(String fieldName, Object fieldValue, int expand) {
        this.fieldName = fieldName;
        // 使用ObjectVO封装字段值，支持对象展开
        this.field = new ObjectVO(fieldValue, expand);
    }

    /**
     * 构造函数 - 用于创建包含匹配类的模型
     *
     * @param matchedClasses 匹配的类集合
     */
    public GetStaticModel(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }

    /**
     * 获取字段值对象
     *
     * @return 字段值对象
     */
    public ObjectVO getField() {
        return field;
    }

    /**
     * 设置字段值对象
     *
     * @param field 字段值对象
     */
    public void setField(ObjectVO field) {
        this.field = field;
    }

    /**
     * 获取字段名称
     *
     * @return 字段名称
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * 设置字段名称
     *
     * @param fieldName 字段名称
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * 获取匹配的类集合
     *
     * @return 匹配的类集合
     */
    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    /**
     * 设置匹配的类集合
     *
     * @param matchedClasses 匹配的类集合
     */
    public void setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }

    /**
     * 获取类加载器的类名
     *
     * @return 类加载器的类名
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名
     * 支持链式调用
     *
     * @param classLoaderClass 类加载器的类名
     * @return 当前对象实例，支持链式调用
     */
    public GetStaticModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    /**
     * 获取匹配的类加载器集合
     *
     * @return 匹配的类加载器集合
     */
    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    /**
     * 设置匹配的类加载器集合
     * 支持链式调用
     *
     * @param matchedClassLoaders 匹配的类加载器集合
     * @return 当前对象实例，支持链式调用
     */
    public GetStaticModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    /**
     * 获取模型类型
     * 用于标识这是一个getstatic命令的结果模型
     *
     * @return 模型类型标识符 "getstatic"
     */
    @Override
    public String getType() {
        return "getstatic";
    }
}
