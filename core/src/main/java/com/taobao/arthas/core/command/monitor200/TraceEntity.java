package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.arthas.core.view.TreeView;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于在ThreadLocal中传递的实体
 *
 * @author ralf0131 2017-01-05 14:05.
 */
public class TraceEntity {

    protected TreeView view;

    protected int deep;

    /**
     * 根节点
     */
    protected Node root;

    /**
     * 当前节点
     */
    protected Node current;

    public TraceEntity() {
        this.root = new Node().markBegin().markEnd();
        this.view = createTreeView();
        this.current = root;
    }

    public TreeView getView() {
        return view;
    }

    public void setView(TreeView view) {
        this.view = view;
    }

    private TreeView createTreeView() {
        String threadTitle = "ts=" + DateUtils.getCurrentDate() + ";" + ThreadUtil.getThreadTitle(Thread.currentThread());
        return new TreeView(true, threadTitle);
    }

    /**
     * 查找耗时最大的叶子节点
     */
    public Node findMaxCostLeaf() {
        return findMaxCostLeaf(this.root, null);
    }

    private Node findMaxCostLeaf(Node node, Node maxCostNode) {
        if (!node.isLeaf()) {
            for (Node n : node.children) {
                maxCostNode = findMaxCostLeaf(n, maxCostNode);
            }
        }
        if (node.isLeaf()) {
            if (maxCostNode == null) {
                maxCostNode = node;
            } else if (maxCostNode.totalCost < node.totalCost) {
                maxCostNode = node;
            }
        }
        return maxCostNode;
    }

    /**
     * 创建一个分支节点
     *
     * @return this
     */
    public TraceEntity begin(String className, String methodName) {
        current = new Node(current, className, methodName);
        current.markBegin();
        view.begin(className + ":" + methodName + "()");
        return this;
    }

    /**
     * 结束一个分支节点
     *
     * @return this
     */
    public TraceEntity end() {
        current.markEnd();
        view.end();
        current = current.parent;
        return this;
    }

    /**
     * 结束一个分支节点,并带上备注
     *
     * @return this
     */
    public TraceEntity end(String mark) {
        current.markEnd();
        view.end(mark);
        current = current.parent;
        return this;
    }

    /**
     * 树节点
     */
    protected static class Node {

        /**
         * 父节点
         */
        Node parent;

        /**
         * 节点类名
         */
        String className;

        /**
         * 节点方法名
         */
        String methodName;

        /**
         * 子节点
         */
        final List<Node> children = new ArrayList<Node>();

        /**
         * 开始时间戳
         */
        private long beginTimestamp;

        /**
         * 结束时间戳
         */
        private long endTimestamp;

        /**
         * 构造树节点(根节点)
         */
        private Node() {
        }

        /**
         * 构造树节点
         *
         * @param parent 父节点
         */
        private Node(Node parent, String className, String methodName) {
            this.parent = parent;
            parent.children.add(this);
            this.className = className;
            this.methodName = methodName;
        }

        /**
         * 是否根节点
         *
         * @return true / false
         */
        boolean isRoot() {
            return null == parent;
        }

        /**
         * 是否叶子节点
         *
         * @return true / false
         */
        boolean isLeaf() {
            return children.isEmpty();
        }

        Node markBegin() {
            beginTimestamp = System.nanoTime();
            return this;
        }

        Node markEnd() {
            endTimestamp = System.nanoTime();

            long cost = getCost();
            if (cost < minCost) {
                minCost = cost;
            }
            if (cost > maxCost) {
                maxCost = cost;
            }
            totalCost += cost;

            return this;
        }

        long getCost() {
            return endTimestamp - beginTimestamp;
        }

        /**
         * 合并统计相同调用,并计算最小\最大\总耗时
         */
        private long minCost = Long.MAX_VALUE;
        private long maxCost = Long.MIN_VALUE;
        private long totalCost = 0;
    }
}
