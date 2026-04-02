package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.List;

/**
 * 内存编译器（Memory Compiler）命令的结果模型类
 *
 * 该类用于封装mc（memory compiler）命令的执行结果，包括编译的文件列表、
 * 类加载器信息等。mc命令用于在内存中编译Java源代码
 * 继承自ResultModel基类，用于命令执行结果的统一管理和传输
 *
 * @author gongdewei 2020/4/20
 */
public class MemoryCompilerModel extends ResultModel {

    /**
     * 编译的文件列表
     * 存储已成功编译的Java源文件的路径列表
     */
    private List<String> files;

    /**
     * 匹配的类加载器集合
     * 存储所有匹配到的类加载器信息
     * 用于确定使用哪个类加载器来加载编译后的类
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 记录当前使用的类加载器的完整类名
     * 指定用于加载编译后类的类加载器类型
     */
    private String classLoaderClass;

    /**
     * 默认构造函数
     * 创建一个空的MemoryCompilerModel实例
     */
    public MemoryCompilerModel() {
    }

    /**
     * 带参数的构造函数
     *
     * @param files 编译的文件列表
     */
    public MemoryCompilerModel(List<String> files) {
        this.files = files;
    }

    /**
     * 设置编译的文件列表
     *
     * @param files 要设置的文件列表
     */
    public void setFiles(List<String> files) {
        this.files = files;
    }

    /**
     * 获取编译的文件列表
     *
     * @return 编译的文件路径列表
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * 获取类加载器的类名
     *
     * @return 类加载器的完整类名
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名
     * 使用链式调用风格，方便连续设置多个属性
     *
     * @param classLoaderClass 类加载器的完整类名
     * @return 当前MemoryCompilerModel实例，支持链式调用
     */
    public MemoryCompilerModel setClassLoaderClass(String classLoaderClass) {
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
     * 使用链式调用风格，方便连续设置多个属性
     *
     * @param matchedClassLoaders 匹配的类加载器集合
     * @return 当前MemoryCompilerModel实例，支持链式调用
     */
    public MemoryCompilerModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    /**
     * 获取模型类型标识
     * 用于在序列化和反序列化时识别模型类型
     * "mc"是memory compiler（内存编译器）的缩写
     *
     * @return 类型标识字符串，固定返回"mc"
     */
    @Override
    public String getType() {
        return "mc";
    }

}
