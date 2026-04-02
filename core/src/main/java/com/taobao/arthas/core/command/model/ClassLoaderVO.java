package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 类加载器值对象（Value Object）
 *
 * 用于表示Java类加载器的相关信息，包括类加载器的名称、哈希值、父类加载器、
 * 加载的类数量、实例数量以及子类加载器列表等。
 * 该类支持树形结构，可以表示类加载器的层级关系。
 *
 * @author gongdewei 2020/4/21
 */
public class ClassLoaderVO {
    /**
     * 类加载器名称
     * 通常为类加载器的类名或自定义的名称
     */
    private String name;

    /**
     * 类加载器哈希值
     * 用于唯一标识一个类加载器实例，通常基于对象的hashCode
     */
    private String hash;

    /**
     * 父类加载器
     * 存储父类加载器的哈希值标识，用于表示类加载器的层级关系
     */
    private String parent;

    /**
     * 已加载的类数量
     * 表示该类加载器已经加载的类的总数
     */
    private Integer loadedCount;

    /**
     * 实例数量
     * 表示该类加载器类型的实例对象数量
     */
    private Integer numberOfInstances;

    /**
     * 子类加载器列表
     * 存储该类加载器的所有子类加载器，用于构建树形结构
     */
    private List<ClassLoaderVO> children;

    /**
     * 默认构造函数
     * 创建一个空的类加载器值对象
     */
    public ClassLoaderVO() {
    }

    /**
     * 添加子类加载器
     * 如果子类加载器列表不存在，会先创建一个新列表
     *
     * @param child 要添加的子类加载器对象
     */
    public void addChild(ClassLoaderVO child){
        // 检查子类加载器列表是否为空
        if (this.children == null){
            // 如果为空，创建一个新的ArrayList来存储子类加载器
            this.children = new ArrayList<ClassLoaderVO>();
        }
        // 将子类加载器添加到列表中
        this.children.add(child);
    }

    /**
     * 获取类加载器名称
     *
     * @return 类加载器名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置类加载器名称
     *
     * @param name 类加载器名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取类加载器哈希值
     *
     * @return 类加载器哈希值字符串
     */
    public String getHash() {
        return hash;
    }

    /**
     * 设置类加载器哈希值
     *
     * @param hash 类加载器哈希值字符串
     */
    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * 获取父类加载器
     *
     * @return 父类加载器的哈希值标识
     */
    public String getParent() {
        return parent;
    }

    /**
     * 设置父类加载器
     *
     * @param parent 父类加载器的哈希值标识
     */
    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
     * 获取已加载的类数量
     *
     * @return 已加载的类数量
     */
    public Integer getLoadedCount() {
        return loadedCount;
    }

    /**
     * 设置已加载的类数量
     *
     * @param loadedCount 已加载的类数量
     */
    public void setLoadedCount(Integer loadedCount) {
        this.loadedCount = loadedCount;
    }

    /**
     * 获取实例数量
     *
     * @return 实例数量
     */
    public Integer getNumberOfInstances() {
        return numberOfInstances;
    }

    /**
     * 设置实例数量
     *
     * @param numberOfInstances 实例数量
     */
    public void setNumberOfInstances(Integer numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    /**
     * 获取子类加载器列表
     *
     * @return 子类加载器列表
     */
    public List<ClassLoaderVO> getChildren() {
        return children;
    }

    /**
     * 设置子类加载器列表
     *
     * @param children 子类加载器列表
     */
    public void setChildren(List<ClassLoaderVO> children) {
        this.children = children;
    }
}
