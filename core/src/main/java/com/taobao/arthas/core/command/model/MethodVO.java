package com.taobao.arthas.core.command.model;

/**
 * 方法或构造器的值对象
 *
 * 用于封装Java方法或构造器的详细信息，包括声明类、方法名、
 * 修饰符、注解、参数、返回类型、异常等元数据信息
 *
 * @author gongdewei 2020/4/9
 */
public class MethodVO {

    /**
     * 声明该方法的类名
     * 表示该方法或构造器所属的类的完全限定名
     */
    private String declaringClass;

    /**
     * 方法名称
     * 对于构造器，此字段为"<init>"
     */
    private String methodName;

    /**
     * 方法修饰符
     * 例如：public、private、protected、static、final等
     */
    private String modifier;

    /**
     * 方法上的注解数组
     * 存储该方法声明的所有注解的完整字符串表示
     */
    private String[] annotations;

    /**
     * 方法参数类型数组
     * 存储方法参数的完全限定类型名称
     */
    private String[] parameters;

    /**
     * 方法返回类型
     * 存储方法返回值的完全限定类型名称
     */
    private String returnType;

    /**
     * 方法抛出的异常类型数组
     * 存储方法throws子句中声明的所有异常的完全限定名称
     */
    private String[] exceptions;

    /**
     * 类加载器的哈希值
     * 用于标识加载该方法所属类的类加载器实例
     */
    private String classLoaderHash;

    /**
     * 方法描述符
     * JVM内部使用的方法签名描述符，遵循JVM规范
     */
    private String descriptor;

    /**
     * 是否为构造器
     * true表示这是一个构造器，false表示这是一个普通方法
     */
    private boolean constructor;

    /**
     * 获取声明该方法的类名
     *
     * @return 类的完全限定名
     */
    public String getDeclaringClass() {
        return declaringClass;
    }

    /**
     * 设置声明该方法的类名
     *
     * @param declaringClass 类的完全限定名
     */
    public void setDeclaringClass(String declaringClass) {
        this.declaringClass = declaringClass;
    }

    /**
     * 获取方法名称
     *
     * @return 方法名称，构造器返回"<init>"
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * 设置方法名称
     *
     * @param methodName 方法名称
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * 获取方法修饰符
     *
     * @return 修饰符字符串
     */
    public String getModifier() {
        return modifier;
    }

    /**
     * 设置方法修饰符
     *
     * @param modifier 修饰符字符串
     */
    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    /**
     * 获取方法上的注解数组
     *
     * @return 注解字符串数组
     */
    public String[] getAnnotations() {
        return annotations;
    }

    /**
     * 设置方法上的注解数组
     *
     * @param annotations 注解字符串数组
     */
    public void setAnnotations(String[] annotations) {
        this.annotations = annotations;
    }

    /**
     * 获取方法参数类型数组
     *
     * @return 参数类型名称数组
     */
    public String[] getParameters() {
        return parameters;
    }

    /**
     * 设置方法参数类型数组
     *
     * @param parameters 参数类型名称数组
     */
    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    /**
     * 获取方法返回类型
     *
     * @return 返回类型的完全限定名
     */
    public String getReturnType() {
        return returnType;
    }

    /**
     * 设置方法返回类型
     *
     * @param returnType 返回类型的完全限定名
     */
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    /**
     * 获取方法抛出的异常类型数组
     *
     * @return 异常类型名称数组
     */
    public String[] getExceptions() {
        return exceptions;
    }

    /**
     * 设置方法抛出的异常类型数组
     *
     * @param exceptions 异常类型名称数组
     */
    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions;
    }

    /**
     * 获取类加载器的哈希值
     *
     * @return 类加载器的哈希值字符串
     */
    public String getClassLoaderHash() {
        return classLoaderHash;
    }

    /**
     * 设置类加载器的哈希值
     *
     * @param classLoaderHash 类加载器的哈希值字符串
     */
    public void setClassLoaderHash(String classLoaderHash) {
        this.classLoaderHash = classLoaderHash;
    }

    /**
     * 判断是否为构造器
     *
     * @return true表示构造器，false表示普通方法
     */
    public boolean isConstructor() {
        return constructor;
    }

    /**
     * 设置是否为构造器
     *
     * @param constructor true表示构造器，false表示普通方法
     */
    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    /**
     * 获取方法描述符
     *
     * @return JVM方法描述符字符串
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * 设置方法描述符
     *
     * @param descriptor JVM方法描述符字符串
     */
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
}
