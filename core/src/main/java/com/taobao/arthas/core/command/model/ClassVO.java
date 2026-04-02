package com.taobao.arthas.core.command.model;

/**
 * 类视图对象（Value Object）
 * <p>
 * 用于表示Java类的详细信息，包括类名、类加载器信息和类加载器哈希值。
 * 主要用于在命令执行结果中展示类的相关信息。
 * </p>
 *
 * @author gongdewei 2020/4/8
 */
public class ClassVO {

    /**
     * 类名
     * 存储类的全限定名（Fully Qualified Name）
     */
    private String name;

    /**
     * 类加载器信息数组
     * 存储类加载器的层次结构信息，从父到子排列
     */
    private String[] classloader;

    /**
     * 类加载器哈希值
     * 用于唯一标识加载该类的类加载器实例
     */
    private String classLoaderHash;

    /**
     * 获取类名
     *
     * @return 类的全限定名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置类名
     *
     * @param name 要设置的类全限定名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取类加载器信息数组
     *
     * @return 类加载器信息数组
     */
    public String[] getClassloader() {
        return classloader;
    }

    /**
     * 设置类加载器信息数组
     *
     * @param classloader 要设置的类加载器信息数组
     */
    public void setClassloader(String[] classloader) {
        this.classloader = classloader;
    }

    /**
     * 获取类加载器哈希值
     *
     * @return 类加载器的哈希值字符串
     */
    public String getClassLoaderHash() {
        return classLoaderHash;
    }

    /**
     * 设置类加载器哈希值
     *
     * @param classLoaderHash 要设置的类加载器哈希值
     */
    public void setClassLoaderHash(String classLoaderHash) {
        this.classLoaderHash = classLoaderHash;
    }
}
