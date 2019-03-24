package com.taobao.arthas.core.view;

import com.taobao.arthas.core.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 树形控件
 * Created by vlinux on 15/5/26.
 */
public class TreeView implements View {

    public static final int INVOKE_ON_BEGIN = 1;
    public static final int INVOKE_AFTER_THROWING = 2;
    public static final int INVOKE_ON_INVOKE_BEFORE = 3;

    private static final String STEP_FIRST_CHAR = "`---";
    private static final String STEP_NORMAL_CHAR = "+---";
    private static final String STEP_HAS_BOARD = "|   ";
    private static final String STEP_EMPTY_BOARD = "    ";
    private static final String TIME_UNIT = "ms";

    // 是否输出耗时
    private final boolean isPrintCost;

    // 根节点
    private final Node root;

    // 当前节点
    private Node current;

    // 最耗时的节点
    private Node maxCost;


    public TreeView(boolean isPrintCost, String title) {
        this.root = new Node(title).markBegin().markEnd();
        this.current = root;
        this.isPrintCost = isPrintCost;
    }

    @Override
    public String draw() {

        rebuildInvokeTree(root);

        findMaxCostNode(root);

        final StringBuilder treeSB = new StringBuilder();

        final Ansi highlighted = Ansi.ansi().fg(Ansi.Color.RED);

        recursive(0, true, "", root, new Callback() {

            @Override
            public void callback(int deep, boolean isLast, String prefix, Node node) {
                treeSB.append(prefix).append(isLast ? STEP_FIRST_CHAR : STEP_NORMAL_CHAR);
                if (isPrintCost && !node.isRoot()) {
                    if (node == maxCost) {
                        // the node with max cost will be highlighted
                        treeSB.append(highlighted.a(node.toString()).reset().toString());
                    } else {
                        treeSB.append(node.toString());
                    }
                }
                treeSB.append(node.data);
                if (!StringUtils.isBlank(node.mark)) {
                    treeSB.append(" [").append(node.mark).append(node.marks > 1 ? "," + node.marks : "").append("]");
                }
                treeSB.append("\n");
            }

        });

        return treeSB.toString();
    }

    private Node rebuildInvokeTree(Node root) {
        Node newRoot = new Node(root.data);
        return null;
    }

    private boolean shouldMergeNodes(Node parent, Node node) {
        //合并重复的结点： merge onBegin to last level onInvokeBefore
        if(node.invokeType == INVOKE_ON_BEGIN && parent.invokeType == INVOKE_ON_INVOKE_BEFORE){

        }
        return false;
    }

    /**
     * 递归遍历
     */
    private void recursive(int deep, boolean isLast, String prefix, Node node, Callback callback) {
        callback.callback(deep, isLast, prefix, node);
        if (!node.isLeaf()) {
            final int size = node.children.size();
            for (int index = 0; index < size; index++) {
                final boolean isLastFlag = index == size - 1;
                final String currentPrefix = isLast ? prefix + STEP_EMPTY_BOARD : prefix + STEP_HAS_BOARD;
                recursive(
                        deep + 1,
                        isLastFlag,
                        currentPrefix,
                        node.children.get(index),
                        callback
                );
            }
        }
    }

    /**
     * 查找耗时最大的节点，便于后续高亮展示
     * @param node
     */
    private void findMaxCostNode(Node node) {
        if (!node.isRoot() && !node.parent.isRoot()) {
            if (maxCost == null) {
                maxCost = node;
            } else if (maxCost.totalCost < node.totalCost) {
                maxCost = node;
            }
        }
        if (!node.isLeaf()) {
            for (Node n: node.children) {
                findMaxCostNode(n);
            }
        }
    }


    /**
     * 创建一个分支节点
     *
     * @param data 节点数据
     * @return this
     */
    public TreeView begin(String data, int invokeType) {
        Node n = current.find(data);
        if (n != null) {
            current = n;
        } else {
            current = new Node(current, data, invokeType);
        }
        current.markBegin();
        return this;
    }

    /**
     * 结束一个分支节点
     *
     * @return this
     */
    public TreeView end() {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.markEnd();
        current = current.parent;
        return this;
    }

    /**
     * 结束一个分支节点,并带上备注
     *
     * @return this
     */
    public TreeView end(String mark) {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.markEnd().mark(mark);
        current = current.parent;
        return this;
    }


    /**
     * 树节点
     */
    private static class Node {

        /**
         * 父节点
         */
        final Node parent;

        /**
         * 节点数据
         */
        final String data;

        /**
         * 子节点
         */
        final List<Node> children = new ArrayList<Node>();

        final Map<String, Node> map = new HashMap<String, Node>();

        /**
         * 开始时间戳
         */
        private long beginTimestamp;

        /**
         * 结束时间戳
         */
        private long endTimestamp;

        /**
         * 备注
         */
        private String mark;

        /**
         * 调用方式
         */
        private int invokeType;

        /**
         * 构造树节点(根节点)
         */
        private Node(String data) {
            this.parent = null;
            this.data = data;
            this.invokeType = INVOKE_ON_BEGIN;
        }

        /**
         * 构造树节点
         *
         * @param parent 父节点
         * @param data   节点数据
         */
        private Node(Node parent, String data, int invokeType) {
            this.parent = parent;
            this.data = data;
            this.invokeType = invokeType;
            parent.children.add(this);
            parent.map.put(data, this);
        }

        Node copy() {
            Node node = new Node(data);
            node.invokeType = invokeType;
            node.beginTimestamp = beginTimestamp;
            node.endTimestamp = endTimestamp;
            node.mark = mark;
            node.marks = marks;
            node.totalCost = totalCost;
            node.maxCost = maxCost;
            node.minCost = minCost;
            node.times = times;
            return node;
        }

        /**
         * 查找已经存在的节点
         */
        Node find(String data) {
            return map.get(data);
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
            times++;
            totalCost += cost;

            return this;
        }

        Node mark(String mark) {
            this.mark = mark;
            marks++;
            return this;
        }

        long getCost() {
            return endTimestamp - beginTimestamp;
        }

        /**
         * convert nano-seconds to milli-seconds
         */
        double getCostInMillis(long nanoSeconds) {
            return nanoSeconds / 1000000.0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (times <= 1) {
                sb.append("[").append(getCostInMillis(getCost())).append(TIME_UNIT).append("] ");
            } else {
                sb.append("[min=").append(getCostInMillis(minCost)).append(TIME_UNIT).append(",max=")
                        .append(getCostInMillis(maxCost)).append(TIME_UNIT).append(",total=")
                        .append(getCostInMillis(totalCost)).append(TIME_UNIT).append(",count=")
                        .append(times).append("] ");
            }
            return sb.toString();
        }

        /**
         * 合并统计相同调用,并计算最小\最大\总耗时
         */
        private long minCost = Long.MAX_VALUE;
        private long maxCost = Long.MIN_VALUE;
        private long totalCost = 0;
        private long times = 0;
        private long marks = 0;
    }


    /**
     * 遍历回调接口
     */
    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, Node node);

    }

}
