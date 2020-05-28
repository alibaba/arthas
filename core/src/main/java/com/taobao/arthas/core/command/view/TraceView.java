package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MethodNode;
import com.taobao.arthas.core.command.model.ThreadNode;
import com.taobao.arthas.core.command.model.TraceModel;
import com.taobao.arthas.core.command.model.TraceNode;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.Ansi;

import java.util.List;


/**
 * @author gongdewei 2020/4/29
 */
public class TraceView extends ResultView<TraceModel> {
    private static final String STEP_FIRST_CHAR = "`---";
    private static final String STEP_NORMAL_CHAR = "+---";
    private static final String STEP_HAS_BOARD = "|   ";
    private static final String STEP_EMPTY_BOARD = "    ";
    private static final String TIME_UNIT = "ms";

    // 是否输出耗时
    private boolean isPrintCost = true;
    private MethodNode maxCostNode;

    @Override
    public void draw(CommandProcess process, TraceModel result) {
        process.write(drawTree(result.getRoot())).write("\n");
    }

    public String drawTree(TraceNode root) {

        //reset status
        maxCostNode = null;
        findMaxCostNode(root);

        final StringBuilder treeSB = new StringBuilder(1024);

        final Ansi highlighted = Ansi.ansi().fg(Ansi.Color.RED);

        recursive(0, true, "", root, new Callback() {

            @Override
            public void callback(int deep, boolean isLast, String prefix, TraceNode node) {
                treeSB.append(prefix).append(isLast ? STEP_FIRST_CHAR : STEP_NORMAL_CHAR);
                renderNode(treeSB, node, highlighted);
                if (!StringUtils.isBlank(node.getMark())) {
                    treeSB.append(" [").append(node.getMark()).append(node.marks() > 1 ? "," + node.marks() : "").append("]");
                }
                treeSB.append("\n");
            }

        });

        return treeSB.toString();
    }

    private void renderNode(StringBuilder sb, TraceNode node, Ansi highlighted) {
        //render cost: [0.366865ms]
        if (isPrintCost && node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;

            if (node == maxCostNode) {
                // the node with max cost will be highlighted
                highlighted.flush(sb);
                renderCost(sb, methodNode);
                highlighted.reset().flush(sb);
            } else {
                renderCost(sb, methodNode);
            }
        }

        //render method name
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            //clazz.getName() + ":" + method.getName() + "()"
            sb.append(methodNode.getClassName()).append(":").append(methodNode.getMethodName()).append("()");
            // #lineNumber
            if (methodNode.getLineNumber()!= -1) {
                sb.append(" #").append(methodNode.getLineNumber());
            }
        } else if (node instanceof ThreadNode) {
            //render thread info
            ThreadNode threadNode = (ThreadNode) node;
            //ts=2020-04-29 10:34:00;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@18b4aac2
            sb.append("ts=").append(DateUtils.formatDate(threadNode.getTimestamp()))
                    .append(";thread_name=").append(threadNode.getThreadName())
                    .append(";id=").append(Long.toHexString(threadNode.getThreadId()))
                    .append(";is_daemon=").append(threadNode.isDaemon())
                    .append(";priority=").append(threadNode.getPriority())
                    .append(";TCCL=").append(threadNode.getClassloader());

            //trace_id
            if (threadNode.getTraceId() != null) {
                sb.append(";trace_id="+threadNode.getTraceId());
            }
            if (threadNode.getRpcId() != null) {
                sb.append(";rpc_id="+threadNode.getRpcId());
            }
        }
    }

    private void renderCost(StringBuilder sb, MethodNode node) {
        if (node.getTimes() <= 1) {
            sb.append("[").append(nanoToMillis(node.getCost())).append(TIME_UNIT).append("] ");
        } else {
            sb.append("[min=").append(nanoToMillis(node.getMinCost())).append(TIME_UNIT).append(",max=")
                    .append(nanoToMillis(node.getMaxCost())).append(TIME_UNIT).append(",total=")
                    .append(nanoToMillis(node.getTotalCost())).append(TIME_UNIT).append(",count=")
                    .append(node.getTimes()).append("] ");
        }
    }

    /**
     * 递归遍历
     */
    private void recursive(int deep, boolean isLast, String prefix, TraceNode node, Callback callback) {
        callback.callback(deep, isLast, prefix, node);
        if (!isLeaf(node)) {
            List<TraceNode> children = node.getChildren();
            if (children == null) {
                return;
            }
            final int size = children.size();
            for (int index = 0; index < size; index++) {
                final boolean isLastFlag = index == size - 1;
                final String currentPrefix = isLast ? prefix + STEP_EMPTY_BOARD : prefix + STEP_HAS_BOARD;
                recursive(
                        deep + 1,
                        isLastFlag,
                        currentPrefix,
                        children.get(index),
                        callback
                );
            }
        }
    }

    /**
     * 查找耗时最大的节点，便于后续高亮展示
     * @param node
     */
    private void findMaxCostNode(TraceNode node) {
        if (node instanceof MethodNode && !isRoot(node) && !isRoot(node.parent())) {
            MethodNode aNode = (MethodNode) node;
            if (maxCostNode == null || maxCostNode.getTotalCost() < aNode.getTotalCost()) {
                maxCostNode = aNode;
            }
        }
        if (!isLeaf(node)) {
            List<TraceNode> children = node.getChildren();
            if (children != null) {
                for (TraceNode n: children) {
                    findMaxCostNode(n);
                }
            }
        }
    }

    private boolean isRoot(TraceNode node) {
        return node.parent() == null;
    }

    private boolean isLeaf(TraceNode node) {
        List<TraceNode> children = node.getChildren();
        return children == null || children.isEmpty();
    }


    /**
     * convert nano-seconds to milli-seconds
     */
    double nanoToMillis(long nanoSeconds) {
        return nanoSeconds / 1000000.0;
    }

    /**
     * 遍历回调接口
     */
    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, TraceNode node);

    }
}
