package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Trace命令的抽象节点模型
 *
 * 该类是Trace命令追踪树结构中的基础抽象节点，提供了节点树的通用属性和方法。
 * Trace命令用于追踪方法调用链路，该类为树形结构中的节点提供基础功能。
 *
 * @author gongdewei 2020/4/28
 */
public abstract class TraceNode {

    /**
     * 父节点引用
     * 用于维护树形结构，指向当前节点的父节点
     */
    protected TraceNode parent;

    /**
     * 子节点列表
     * 存储当前节点的所有子节点，维护树形结构的层级关系
     */
    protected List<TraceNode> children;

    /**
     * 节点类型
     * 标识节点的类型，例如：method（方法节点）、thread（线程节点）等
     */
    private String type;

    /**
     * 节点备注信息
     * 用于存储节点的额外说明信息，比如异常信息、特殊标记等
     */
    private String mark;

    /**
     * 标记计数
     * 记录该节点被标记的次数，每次调用setMark方法时该计数器会递增
     * TODO marks数量的作用？是否可以去掉
     */
    private int marks = 0;

    /**
     * 构造函数
     *
     * @param type 节点类型字符串，用于标识该节点的类型
     */
    public TraceNode(String type) {
        this.type = type;
    }

    /**
     * 添加子节点
     *
     * 将指定的节点添加为当前节点的子节点，同时设置子节点的父节点引用为当前节点
     *
     * @param child 要添加的子节点
     */
    public void addChild(TraceNode child) {
        // 如果子节点列表未初始化，则先创建
        if (children == null) {
            children = new ArrayList<TraceNode>();
        }
        // 将子节点添加到列表中
        this.children.add(child);
        // 设置子节点的父节点为当前节点
        child.setParent(this);
    }

    /**
     * 设置节点备注信息
     *
     * 设置节点的备注内容，同时递增标记计数器
     *
     * @param mark 备注信息字符串
     */
    public void setMark(String mark) {
        this.mark = mark;
        // 每次设置备注时，标记计数器递增
        marks++;
    }

    /**
     * 获取节点备注信息
     *
     * @return 备注信息字符串，如果没有设置则返回null
     */
    public String getMark() {
        return mark;
    }

    /**
     * 获取标记计数
     *
     * @return 标记次数
     */
    public Integer marks() {
        return marks;
    }

    /**
     * 节点开始时的钩子方法
     *
     * 子类可以重写此方法以实现节点开始时的特定逻辑，
     * 例如记录开始时间、初始化状态等
     */
    public void begin() {
    }

    /**
     * 节点结束时的钩子方法
     *
     * 子类可以重写此方法以实现节点结束时的特定逻辑，
     * 例如计算耗时、清理资源等
     */
    public void end() {
    }

    /**
     * 获取节点类型
     *
     * @return 节点类型字符串
     */
    public String getType() {
        return type;
    }

    /**
     * 设置节点类型
     *
     * @param type 节点类型字符串
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取父节点
     *
     * @return 父节点对象，如果没有父节点则返回null
     */
    public TraceNode parent() {
        return parent;
    }

    /**
     * 设置父节点
     *
     * @param parent 要设置的父节点对象
     */
    public void setParent(TraceNode parent) {
        this.parent = parent;
    }

    /**
     * 获取子节点列表
     *
     * @return 子节点列表，如果没有子节点则返回null
     */
    public List<TraceNode> getChildren() {
        return children;
    }
}
