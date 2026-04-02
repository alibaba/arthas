package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.NavigableMap;

/**
 * Jad(Java反编译)命令结果模型
 * 用于封装jad命令执行后返回的反编译结果信息
 *
 * @author gongdewei 2020/4/22
 * @author hengyunabc 2021-02-23
 */
public class JadModel extends ResultModel {
    /**
     * 类信息对象
     * 包含被反编译类的详细信息，如类名、父类、接口等
     */
    private ClassVO classInfo;

    /**
     * 类文件位置
     * 指示被反编译的类文件所在的路径或URL
     */
    private String location;

    /**
     * 反编译后的源代码
     * 存储反编译后的Java源代码字符串
     */
    private String source;

    /**
     * 行号映射关系
     * 键: 反编译后的行号, 值: 原始字节码行号
     * 用于建立反编译代码与原始字节码之间的行号对应关系
     */
    private NavigableMap<Integer,Integer> mappings;

    /**
     * 匹配的类加载器集合
     * 当匹配到多个类加载器时，存储所有符合条件的类加载器信息
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 记录加载该类的类加载器的类名
     */
    private String classLoaderClass;

    /**
     * 匹配的多个类集合
     * 当 Jad 命令匹配到多个类时，存储所有匹配的类信息
     */
    private Collection<ClassVO> matchedClasses;

    /**
     * 获取结果类型
     * 用于标识该模型对应的命令类型
     *
     * @return 返回"jad"字符串标识
     */
    @Override
    public String getType() {
        return "jad";
    }

    /**
     * 默认构造函数
     * 创建一个空的JadModel实例
     */
    public JadModel() {
    }

    /**
     * 获取类信息
     *
     * @return 返回类的详细信息对象
     */
    public ClassVO getClassInfo() {
        return classInfo;
    }

    /**
     * 设置类信息
     *
     * @param classInfo 要设置的类信息对象
     */
    public void setClassInfo(ClassVO classInfo) {
        this.classInfo = classInfo;
    }

    /**
     * 获取类文件位置
     *
     * @return 返回类文件的路径或URL
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置类文件位置
     *
     * @param location 类文件的路径或URL
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 获取反编译后的源代码
     *
     * @return 返回反编译后的Java源代码字符串
     */
    public String getSource() {
        return source;
    }

    /**
     * 设置反编译后的源代码
     *
     * @param source 反编译后的Java源代码字符串
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * 获取行号映射关系
     *
     * @return 返回反编译行号到原始字节码行号的映射
     */
    public NavigableMap<Integer, Integer> getMappings() {
        return mappings;
    }

    /**
     * 设置行号映射关系
     *
     * @param mappings 反编译行号到原始字节码行号的映射
     */
    public void setMappings(NavigableMap<Integer, Integer> mappings) {
        this.mappings = mappings;
    }

    /**
     * 获取匹配的类集合
     *
     * @return 返回所有匹配的类信息集合
     */
    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    /**
     * 设置匹配的类集合
     *
     * @param matchedClasses 要设置的匹配类集合
     */
    public void setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }

    /**
     * 获取类加载器的类名
     *
     * @return 返回类加载器的类名字符串
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名
     * 采用链式调用设计，方便连续设置多个属性
     *
     * @param classLoaderClass 要设置的类加载器类名
     * @return 返回当前JadModel实例，支持链式调用
     */
    public JadModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    /**
     * 获取匹配的类加载器集合
     *
     * @return 返回所有匹配的类加载器信息集合
     */
    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    /**
     * 设置匹配的类加载器集合
     * 采用链式调用设计，方便连续设置多个属性
     *
     * @param matchedClassLoaders 要设置的匹配类加载器集合
     * @return 返回当前JadModel实例，支持链式调用
     */
    public JadModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
