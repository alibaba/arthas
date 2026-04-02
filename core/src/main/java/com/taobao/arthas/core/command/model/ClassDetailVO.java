package com.taobao.arthas.core.command.model;

/**
 * 类详情值对象（Value Object）
 *
 * 继承自ClassVO，用于展示Java类的详细信息。
 * 包含了类的各种元数据信息，如类类型标识、注解、接口、父类、字段等信息。
 * 主要用于在命令执行结果中展示类的完整结构信息。
 *
 * @author gongdewei 2020/4/8
 */
public class ClassDetailVO extends ClassVO {

    /**
     * 类信息字符串
     * 包含类的基本描述信息
     */
    private String classInfo;

    /**
     * 代码来源
     * 表示类所在的代码源位置，如jar包路径或文件路径
     */
    private String codeSource;

    /**
     * 是否为接口
     * 标识当前类是否是一个接口类型
     */
    private boolean isInterface;

    /**
     * 是否为注解类型
     * 标识当前类是否是一个注解（Annotation）
     */
    private boolean isAnnotation;

    /**
     * 是否为枚举类型
     * 标识当前类是否是一个枚举（Enum）
     */
    private boolean isEnum;

    /**
     * 是否为匿名类
     * 标识当前类是否是一个匿名内部类
     */
    private boolean isAnonymousClass;

    /**
     * 是否为数组类型
     * 标识当前类是否是一个数组
     */
    private boolean isArray;

    /**
     * 是否为局部类
     * 标识当前类是否是在方法内部定义的局部类
     */
    private boolean isLocalClass;

    /**
     * 是否为成员类
     * 标识当前类是否是一个成员类（定义在另一个类内部的类）
     */
    private boolean isMemberClass;

    /**
     * 是否为基本类型
     * 标识当前类是否是一个Java基本类型（如int、boolean等）
     */
    private boolean isPrimitive;

    /**
     * 是否为合成类
     * 标识当前类是否是编译器自动生成的合成类
     */
    private boolean isSynthetic;

    /**
     * 简单类名
     * 不包含包路径的类名
     */
    private String simpleName;

    /**
     * 访问修饰符
     * 如public、private、protected等
     */
    private String modifier;

    /**
     * 类上的注解列表
     * 存储该类声明的所有注解的字符串表示
     */
    private String[] annotations;

    /**
     * 实现的接口列表
     * 存储该类实现的所有接口的完整类名
     */
    private String[] interfaces;

    /**
     * 父类列表
     * 存储该类的继承层级，通常包含直接父类的完整类名
     */
    private String[] superClass;

    /**
     * 字段列表
     * 存储该类声明的所有字段的详细信息
     */
    private FieldVO[] fields;

    /**
     * 获取类信息字符串
     *
     * @return 类信息描述
     */
    public String getClassInfo() {
        return classInfo;
    }

    /**
     * 设置类信息字符串
     *
     * @param classInfo 类信息描述
     */
    public void setClassInfo(String classInfo) {
        this.classInfo = classInfo;
    }

    /**
     * 获取代码来源
     *
     * @return 代码来源路径
     */
    public String getCodeSource() {
        return codeSource;
    }

    /**
     * 设置代码来源
     *
     * @param codeSource 代码来源路径
     */
    public void setCodeSource(String codeSource) {
        this.codeSource = codeSource;
    }

    /**
     * 判断是否为接口
     *
     * @return 如果是接口返回true，否则返回false
     */
    public boolean isInterface() {
        return isInterface;
    }

    /**
     * 设置是否为接口
     *
     * @param anInterface 是否为接口
     */
    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }

    /**
     * 判断是否为注解类型
     *
     * @return 如果是注解类型返回true，否则返回false
     */
    public boolean isAnnotation() {
        return isAnnotation;
    }

    /**
     * 设置是否为注解类型
     *
     * @param annotation 是否为注解类型
     */
    public void setAnnotation(boolean annotation) {
        isAnnotation = annotation;
    }

    /**
     * 判断是否为枚举类型
     *
     * @return 如果是枚举类型返回true，否则返回false
     */
    public boolean isEnum() {
        return isEnum;
    }

    /**
     * 设置是否为枚举类型
     *
     * @param anEnum 是否为枚举类型
     */
    public void setEnum(boolean anEnum) {
        isEnum = anEnum;
    }

    /**
     * 判断是否为匿名类
     *
     * @return 如果是匿名类返回true，否则返回false
     */
    public boolean isAnonymousClass() {
        return isAnonymousClass;
    }

    /**
     * 设置是否为匿名类
     *
     * @param anonymousClass 是否为匿名类
     */
    public void setAnonymousClass(boolean anonymousClass) {
        isAnonymousClass = anonymousClass;
    }

    /**
     * 判断是否为数组类型
     *
     * @return 如果是数组类型返回true，否则返回false
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * 设置是否为数组类型
     *
     * @param array 是否为数组类型
     */
    public void setArray(boolean array) {
        isArray = array;
    }

    /**
     * 判断是否为局部类
     *
     * @return 如果是局部类返回true，否则返回false
     */
    public boolean isLocalClass() {
        return isLocalClass;
    }

    /**
     * 设置是否为局部类
     *
     * @param localClass 是否为局部类
     */
    public void setLocalClass(boolean localClass) {
        isLocalClass = localClass;
    }

    /**
     * 判断是否为成员类
     *
     * @return 如果是成员类返回true，否则返回false
     */
    public boolean isMemberClass() {
        return isMemberClass;
    }

    /**
     * 设置是否为成员类
     *
     * @param memberClass 是否为成员类
     */
    public void setMemberClass(boolean memberClass) {
        isMemberClass = memberClass;
    }

    /**
     * 判断是否为基本类型
     *
     * @return 如果是基本类型返回true，否则返回false
     */
    public boolean isPrimitive() {
        return isPrimitive;
    }

    /**
     * 设置是否为基本类型
     *
     * @param primitive 是否为基本类型
     */
    public void setPrimitive(boolean primitive) {
        isPrimitive = primitive;
    }

    /**
     * 判断是否为合成类
     *
     * @return 如果是合成类返回true，否则返回false
     */
    public boolean isSynthetic() {
        return isSynthetic;
    }

    /**
     * 设置是否为合成类
     *
     * @param synthetic 是否为合成类
     */
    public void setSynthetic(boolean synthetic) {
        isSynthetic = synthetic;
    }

    /**
     * 获取简单类名
     *
     * @return 不包含包路径的类名
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * 设置简单类名
     *
     * @param simpleName 不包含包路径的类名
     */
    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    /**
     * 获取访问修饰符
     *
     * @return 访问修饰符字符串
     */
    public String getModifier() {
        return modifier;
    }

    /**
     * 设置访问修饰符
     *
     * @param modifier 访问修饰符字符串
     */
    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    /**
     * 获取类上的注解列表
     *
     * @return 注解字符串数组
     */
    public String[] getAnnotations() {
        return annotations;
    }

    /**
     * 设置类上的注解列表
     *
     * @param annotations 注解字符串数组
     */
    public void setAnnotations(String[] annotations) {
        this.annotations = annotations;
    }

    /**
     * 获取实现的接口列表
     *
     * @return 接口完整类名数组
     */
    public String[] getInterfaces() {
        return interfaces;
    }

    /**
     * 设置实现的接口列表
     *
     * @param interfaces 接口完整类名数组
     */
    public void setInterfaces(String[] interfaces) {
        this.interfaces = interfaces;
    }

    /**
     * 获取父类列表
     *
     * @return 父类完整类名数组
     */
    public String[] getSuperClass() {
        return superClass;
    }

    /**
     * 设置父类列表
     *
     * @param superClass 父类完整类名数组
     */
    public void setSuperClass(String[] superClass) {
        this.superClass = superClass;
    }

    /**
     * 获取字段列表
     *
     * @return 字段信息数组
     */
    public FieldVO[] getFields() {
        return fields;
    }

    /**
     * 设置字段列表
     *
     * @param fields 字段信息数组
     */
    public void setFields(FieldVO[] fields) {
        this.fields = fields;
    }

}
