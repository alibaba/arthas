package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MethodNode;
import com.taobao.arthas.core.command.model.ThreadNode;
import com.taobao.arthas.core.command.model.ThrowNode;
import com.taobao.arthas.core.command.model.TraceModel;
import com.taobao.arthas.core.command.model.TraceNode;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.Ansi;

import java.util.List;

import static java.lang.String.format;

/**
 * Trace命令的终端视图类
 * 负责将方法调用链路渲染为树形结构，显示每个方法的调用耗时、占比等信息
 * 支持高亮显示耗时最长的节点
 *
 * @author gongdewei 2020/4/29
 */
public class TraceView extends ResultView<TraceModel> {
    // 树形结构中最后一个子节点的连接符号
    private static final String STEP_FIRST_CHAR = "`---";
    // 树形结构中普通子节点的连接符号
    private static final String STEP_NORMAL_CHAR = "+---";
    // 树形结构中非最后分支的竖线符号
    private static final String STEP_HAS_BOARD = "|   ";
    // 树形结构中最后分支的空格符号
    private static final String STEP_EMPTY_BOARD = "    ";
    // 时间单位：毫秒
    private static final String TIME_UNIT = "ms";
    // 百分比符号
    private static final char PERCENTAGE = '%';

    // 是否输出耗时信息（默认为true）
    private boolean isPrintCost = true;
    // 记录耗时最大的方法节点，用于高亮显示
    private MethodNode maxCostNode;

    /**
     * 绘制Trace视图
     * 将方法调用链路树形结构输出到终端
     *
     * @param process 命令处理进程，用于输出结果
     * @param result Trace模型对象，包含方法调用链路的根节点
     */
    @Override
    public void draw(CommandProcess process, TraceModel result) {
        process.write(drawTree(result.getRoot())).write("\n");
    }

    /**
     * 绘制方法调用树
     * 递归遍历所有节点，生成树形结构的字符串表示
     *
     * @param root 调用链路的根节点
     * @return 格式化后的树形结构字符串
     */
    public String drawTree(TraceNode root) {

        // 重置状态，清空之前的最大耗时节点记录
        maxCostNode = null;
        // 查找整棵树中耗时最大的方法节点
        findMaxCostNode(root);

        final StringBuilder treeSB = new StringBuilder(2048);

        // 创建红色高亮样式，用于突出显示耗时最大的节点
        final Ansi highlighted = Ansi.ansi().fg(Ansi.Color.RED);

        // 递归遍历树形结构，通过回调函数构建输出字符串
        recursive(0, true, "", root, new Callback() {

            /**
             * 回调函数，处理每个节点的渲染
             *
             * @param deep 节点深度
             * @param isLast 是否是父节点的最后一个子节点
             * @param prefix 前缀字符串（用于构建树形结构）
             * @param node 当前节点
             */
            @Override
            public void callback(int deep, boolean isLast, String prefix, TraceNode node) {
                // 添加前缀和连接符
                treeSB.append(prefix).append(isLast ? STEP_FIRST_CHAR : STEP_NORMAL_CHAR);
                // 渲染节点内容（耗时、方法名等）
                renderNode(treeSB, node, highlighted);
                // 如果节点有标记信息（如条件断点），则显示标记
                if (!StringUtils.isBlank(node.getMark())) {
                    treeSB.append(" [").append(node.getMark()).append(node.marks() > 1 ? "," + node.marks() : "").append("]");
                }
                treeSB.append("\n");
            }

        });

        return treeSB.toString();
    }

    /**
     * 渲染单个节点
     * 根据节点类型（方法节点、线程节点、异常节点）渲染不同的内容
     *
     * @param sb 字符串构建器
     * @param node 要渲染的节点
     * @param highlighted 高亮样式对象，用于高亮显示耗时最大的节点
     */
    private void renderNode(StringBuilder sb, TraceNode node, Ansi highlighted) {
        // 渲染耗时信息：[0.366865ms] 或 [25.50% 0.366865ms]
        if (isPrintCost && node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;

            // 获取格式化后的耗时字符串
            String costStr = renderCost(methodNode);
            if (node == maxCostNode) {
                // 如果是耗时最大的节点，使用红色高亮显示
                sb.append(highlighted.a(costStr).reset().toString());
            } else {
                sb.append(costStr);
            }
        }

        // 渲染方法名称
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            // 格式：类名:方法名()
            sb.append(methodNode.getClassName()).append(":").append(methodNode.getMethodName()).append("()");
            // 如果有行号信息，添加行号：#123
            if (methodNode.getLineNumber()!= -1) {
                sb.append(" #").append(methodNode.getLineNumber());
            }
        } else if (node instanceof ThreadNode) {
            // 渲染线程节点信息
            ThreadNode threadNode = (ThreadNode) node;
            // 格式：ts=2020-04-29 10:34:00;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=xxx
            sb.append(format("ts=%s;thread_name=%s;id=%d;is_daemon=%s;priority=%d;TCCL=%s",
                    DateUtils.formatDateTime(threadNode.getTimestamp()),
                    threadNode.getThreadName(),
                    threadNode.getThreadId(),
                    threadNode.isDaemon(),
                    threadNode.getPriority(),
                    threadNode.getClassloader()));

            // 如果存在trace_id（分布式追踪ID），则显示
            if (threadNode.getTraceId() != null) {
                sb.append(";trace_id=").append(threadNode.getTraceId());
            }
            // 如果存在rpc_id（RPC调用ID），则显示
            if (threadNode.getRpcId() != null) {
                sb.append(";rpc_id=").append(threadNode.getRpcId());
            }
        } else if (node instanceof ThrowNode) {
            // 渲染异常节点
            ThrowNode throwNode = (ThrowNode) node;
            // 格式：throw:异常类名 #行号 [异常消息]
            sb.append("throw:").append(throwNode.getException())
                    .append(" #").append(throwNode.getLineNumber())
                    .append(" [").append(throwNode.getMessage()).append("]");

        } else {
            // 未知节点类型，抛出异常
            throw new UnsupportedOperationException("unknown trace node: " + node.getClass());
        }
    }

    /**
     * 渲染方法的耗时信息
     * 根据方法调用次数和父节点类型，生成不同格式的耗时字符串
     *
     * @param node 方法节点
     * @return 格式化后的耗时字符串
     */
    private String renderCost(MethodNode node) {
        StringBuilder sb = new StringBuilder();
        if (node.getTimes() <= 1) {
            // 方法只调用一次的情况
            if(node.parent() instanceof ThreadNode) {
                // 父节点是线程节点，显示绝对耗时：[0.366865ms]
                sb.append('[').append(nanoToMillis(node.getCost())).append(TIME_UNIT).append("] ");
            }else {
                // 父节点是方法节点，显示相对耗时和占比：[25.50% 0.366865ms]
                MethodNode parentNode = (MethodNode) node.parent();
                String percentage = String.format("%.2f", node.getCost()*100.0/parentNode.getTotalCost());
                sb.append('[').append(percentage).append(PERCENTAGE).append(" ").append(nanoToMillis(node.getCost())).append(TIME_UNIT).append(" ").append("] ");

            }
        } else {
            // 方法调用多次的情况，显示统计信息：最小值、最大值、总值、调用次数
            if(node.parent() instanceof ThreadNode) {
                // 父节点是线程节点，不显示百分比
                sb.append("[min=").append(nanoToMillis(node.getMinCost())).append(TIME_UNIT).append(",max=")
                        .append(nanoToMillis(node.getMaxCost())).append(TIME_UNIT).append(",total=")
                        .append(nanoToMillis(node.getTotalCost())).append(TIME_UNIT).append(",count=")
                        .append(node.getTimes()).append("] ");
            }else {
                // 父节点是方法节点，显示总耗时占比和统计信息
                MethodNode parentNode = (MethodNode) node.parent();
                String percentage = String.format("%.2f",node.getTotalCost()*100.0/parentNode.getTotalCost());
                sb.append('[').append(percentage).append(PERCENTAGE).append(" min=").append(nanoToMillis(node.getMinCost())).append(TIME_UNIT).append(",max=")
                        .append(nanoToMillis(node.getMaxCost())).append(TIME_UNIT).append(",total=")
                        .append(nanoToMillis(node.getTotalCost())).append(TIME_UNIT).append(",count=")
                        .append(node.getTimes()).append("] ");

            }

        }
        return sb.toString();
    }

    /**
     * 递归遍历树形结构
     * 深度优先遍历所有节点，对每个节点执行回调操作
     *
     * @param deep 当前节点深度
     * @param isLast 是否是父节点的最后一个子节点
     * @param prefix 前缀字符串（用于构建树形结构的缩进和连接线）
     * @param node 当前要处理的节点
     * @param callback 回调接口，在遍历每个节点时调用
     */
    private void recursive(int deep, boolean isLast, String prefix, TraceNode node, Callback callback) {
        // 执行回调，处理当前节点
        callback.callback(deep, isLast, prefix, node);
        // 如果不是叶子节点，继续递归处理子节点
        if (!isLeaf(node)) {
            List<TraceNode> children = node.getChildren();
            if (children == null) {
                return;
            }
            final int size = children.size();
            // 遍历所有子节点
            for (int index = 0; index < size; index++) {
                final boolean isLastFlag = index == size - 1;
                // 根据当前节点是否是最后一个子节点，决定子节点的前缀
                // 最后一个子节点使用空格前缀，其他使用竖线前缀
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
     * 查找耗时最大的节点
     * 遍历整棵树，找出总耗时最大的方法节点，便于后续高亮展示
     * 注意：排除根节点和根节点的直接子节点（它们通常是线程节点或顶层调用）
     *
     * @param node 当前检查的节点
     */
    private void findMaxCostNode(TraceNode node) {
        // 只统计方法节点，且排除根节点和根节点的直接子节点
        if (node instanceof MethodNode && !isRoot(node) && !isRoot(node.parent())) {
            MethodNode aNode = (MethodNode) node;
            if (maxCostNode == null || maxCostNode.getTotalCost() < aNode.getTotalCost()) {
                maxCostNode = aNode;
            }
        }
        // 递归检查子节点
        if (!isLeaf(node)) {
            List<TraceNode> children = node.getChildren();
            if (children != null) {
                for (TraceNode n: children) {
                    findMaxCostNode(n);
                }
            }
        }
    }

    /**
     * 判断节点是否为根节点
     *
     * @param node 要检查的节点
     * @return 如果是根节点返回true，否则返回false
     */
    private boolean isRoot(TraceNode node) {
        return node.parent() == null;
    }

    /**
     * 判断节点是否为叶子节点
     *
     * @param node 要检查的节点
     * @return 如果是叶子节点（没有子节点）返回true，否则返回false
     */
    private boolean isLeaf(TraceNode node) {
        List<TraceNode> children = node.getChildren();
        return children == null || children.isEmpty();
    }


    /**
     * 将纳秒转换为毫秒
     *
     * @param nanoSeconds 纳秒值
     * @return 毫秒值
     */
    double nanoToMillis(long nanoSeconds) {
        return nanoSeconds / 1000000.0;
    }

    /**
     * 遍历回调接口
     * 用于在递归遍历树形结构时，对每个节点执行自定义操作
     */
    private interface Callback {

        /**
         * 回调方法
         *
         * @param deep 节点深度
         * @param isLast 是否是父节点的最后一个子节点
         * @param prefix 前缀字符串
         * @param node 当前节点
         */
        void callback(int deep, boolean isLast, String prefix, TraceNode node);

    }
}
