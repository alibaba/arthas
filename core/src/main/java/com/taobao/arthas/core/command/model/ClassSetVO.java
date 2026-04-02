package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * 类集合视图对象（Value Object）
 * <p>
 * 用于表示类加载器及其加载的类集合，主要用于返回类搜索结果。
 * 该类实现了Countable接口，可以统计集合中的类数量。
 * </p>
 *
 * @author gongdewei 2020/4/21
 */
public class ClassSetVO implements Countable {
    /**
     * 类加载器视图对象
     * 表示加载这些类的类加载器信息
     */
    private ClassLoaderVO classloader;

    /**
     * 类名集合
     * 存储该类加载器加载的所有类的全限定名
     */
    private Collection<String> classes;

    /**
     * 分段索引
     * 用于分批返回大量类数据时的分段标记，0表示第一段或唯一一段
     */
    private int segment;

    /**
     * 构造函数
     * 创建一个类集合视图对象，默认使用第0段
     *
     * @param classloader 类加载器视图对象
     * @param classes     类名集合
     */
    public ClassSetVO(ClassLoaderVO classloader, Collection<String> classes) {
        // 调用三参数构造函数，默认段索引为0
        this(classloader, classes, 0);
    }

    /**
     * 完整构造函数
     * 创建一个类集合视图对象，指定类加载器、类集合和分段索引
     *
     * @param classloader 类加载器视图对象
     * @param classes     类名集合
     * @param segment     分段索引，用于标识这是第几批数据
     */
    public ClassSetVO(ClassLoaderVO classloader, Collection<String> classes, int segment) {
        // 保存类加载器信息
        this.classloader = classloader;
        // 保存类名集合
        this.classes = classes;
        // 保存分段索引
        this.segment = segment;
    }

    /**
     * 获取类加载器视图对象
     *
     * @return 类加载器视图对象
     */
    public ClassLoaderVO getClassloader() {
        return classloader;
    }

    /**
     * 设置类加载器视图对象
     *
     * @param classloader 要设置的类加载器视图对象
     */
    public void setClassloader(ClassLoaderVO classloader) {
        this.classloader = classloader;
    }

    /**
     * 获取类名集合
     *
     * @return 类名集合
     */
    public Collection<String> getClasses() {
        return classes;
    }

    /**
     * 设置类名集合
     *
     * @param classes 要设置的类名集合
     */
    public void setClasses(Collection<String> classes) {
        this.classes = classes;
    }

    /**
     * 获取分段索引
     *
     * @return 分段索引
     */
    public int getSegment() {
        return segment;
    }

    /**
     * 设置分段索引
     *
     * @param segment 要设置的分段索引
     */
    public void setSegment(int segment) {
        this.segment = segment;
    }

    /**
     * 计算集合大小
     * 实现Countable接口的方法，返回集合中类的数量
     *
     * @return 集合中类的数量，如果集合为null则返回1（表示有一个类集合对象）
     */
    @Override
    public int size() {
        // 如果类集合不为null，返回集合大小；否则返回1（表示有一个ClassSetVO对象）
        return classes != null ? classes.size() : 1;
    }
}
