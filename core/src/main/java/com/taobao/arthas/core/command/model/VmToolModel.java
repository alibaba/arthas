package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * VmTool命令结果模型类
 * 用于封装vmtool命令的执行结果，包括返回值、匹配的类加载器等信息
 *
 * @author hengyunabc 2022-04-24
 */
public class VmToolModel extends ResultModel {
    /**
     * 命令执行结果的返回值对象
     * 包含实际执行后返回的数据
     */
    private ObjectVO value;

    /**
     * 匹配到的类加载器集合
     * 存储所有符合查询条件的类加载器对象
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 指定使用的类加载器类型的全限定名
     */
    private String classLoaderClass;


    /**
     * 获取结果模型的类型标识
     *
     * @return 类型标识字符串 "vmtool"
     */
    @Override
    public String getType() {
        return "vmtool";
    }

    /**
     * 获取命令执行的返回值
     *
     * @return 返回值对象，包含执行结果数据
     */
    public ObjectVO getValue() {
        return value;
    }

    /**
     * 设置命令执行的返回值
     * 使用链式调用风格，方便连续设置多个属性
     *
     * @param value 返回值对象，包含执行结果数据
     * @return 当前VmToolModel对象实例，支持链式调用
     */
    public VmToolModel setValue(ObjectVO value) {
        this.value = value;
        return this;
    }

    /**
     * 获取类加载器的类名
     *
     * @return 类加载器的全限定类名
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名
     * 使用链式调用风格，方便连续设置多个属性
     *
     * @param classLoaderClass 类加载器的全限定类名
     * @return 当前VmToolModel对象实例，支持链式调用
     */
    public VmToolModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    /**
     * 获取匹配的类加载器集合
     *
     * @return 匹配到的类加载器视图对象集合
     */
    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    /**
     * 设置匹配的类加载器集合
     * 使用链式调用风格，方便连续设置多个属性
     *
     * @param matchedClassLoaders 匹配到的类加载器视图对象集合
     * @return 当前VmToolModel对象实例，支持链式调用
     */
    public VmToolModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
