package com.taobao.arthas.core.command.model;

/**
 * Trace命令的数据模型
 * 用于Trace命令的输出，该命令可以追踪方法调用链路，展示方法调用的完整调用栈
 * TraceModel维护了一个树形结构，根节点代表调用的起点，子节点代表后续的方法调用
 *
 * @author gongdewei 2020/4/29
 */
public class TraceModel extends ResultModel {
    /** 追踪树的根节点，代表方法调用链的起始点 */
    private TraceNode root;

    /** 节点总数，记录整个追踪树中包含的节点数量 */
    private int nodeCount;

    /**
     * 默认构造函数
     * 创建一个空的TraceModel实例
     */
    public TraceModel() {
    }

    /**
     * 带参数的构造函数
     *
     * @param root      追踪树的根节点
     * @param nodeCount 追踪树中的节点总数
     */
    public TraceModel(TraceNode root, int nodeCount) {
        this.root = root;
        this.nodeCount = nodeCount;
    }

    /**
     * 获取模型类型
     * 用于标识这是Trace命令的输出模型
     *
     * @return 模型类型标识字符串 "trace"
     */
    @Override
    public String getType() {
        return "trace";
    }

    /**
     * 获取追踪树的根节点
     * 根节点是整个方法调用链的起点，通过遍历根节点及其子节点可以获取完整的调用链路
     *
     * @return 追踪树的根节点
     */
    public TraceNode getRoot() {
        return root;
    }

    /**
     * 设置追踪树的根节点
     *
     * @param root 追踪树的根节点
     */
    public void setRoot(TraceNode root) {
        this.root = root;
    }

    /**
     * 获取节点总数
     * 追踪树中所有节点的数量，包括根节点和所有子节点
     *
     * @return 节点总数
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * 设置节点总数
     *
     * @param nodeCount 节点总数
     */
    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }
}
