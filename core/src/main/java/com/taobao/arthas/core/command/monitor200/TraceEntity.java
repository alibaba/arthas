package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.model.TraceModel;
import com.taobao.arthas.core.command.model.TraceTree;
import com.taobao.arthas.core.util.ThreadUtil;

/**
 * Trace追踪实体类
 * 用于在ThreadLocal中传递方法调用链路的追踪信息
 * 每个线程都有自己独立的TraceEntity实例，用于记录该线程的方法调用树
 *
 * @author ralf0131 2017-01-05 14:05.
 */
public class TraceEntity {

    /**
     * 调用链路树
     * 用于记录方法调用的层次结构和执行时间
     */
    protected TraceTree tree;

    /**
     * 调用深度
     * 记录当前方法调用的嵌套层级
     */
    protected int deep;

    /**
     * 构造函数
     * 初始化追踪实体，创建调用链路树
     *
     * @param loader 类加载器
     */
    public TraceEntity(ClassLoader loader) {
        this.tree = createTraceTree(loader);
        this.deep = 0;
    }

    /**
     * 创建追踪树
     * 使用当前线程的信息作为追踪树的根节点
     *
     * @param loader 类加载器
     * @return 追踪树对象
     */
    private TraceTree createTraceTree(ClassLoader loader) {
        return new TraceTree(ThreadUtil.getThreadNode(loader, Thread.currentThread()));
    }

    /**
     * 获取追踪模型
     * 对追踪树进行修剪（去除不需要的节点），然后构建追踪模型用于展示
     *
     * @return 追踪模型，包含根节点和节点总数
     */
    public TraceModel getModel() {
        // 修剪追踪树，移除不需要的节点
        tree.trim();
        // 构建并返回追踪模型
        return new TraceModel(tree.getRoot(), tree.getNodeCount());
    }
}
