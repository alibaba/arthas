package com.taobao.arthas.core.command.model;


import com.taobao.arthas.core.util.StringUtils;

import java.util.List;

/**
 * Trace命令的树形模型
 *
 * 该类用于构建和管理Trace命令追踪过程中形成的方法调用树。
 * TraceTree维护了方法调用的层级关系，支持方法调用的开始、结束、异常等事件的记录。
 * 树形结构能够清晰地展示方法调用的完整链路。
 *
 * @author gongdewei 2020/4/28
 */
public class TraceTree {

    /**
     * 树的根节点
     * 通常是ThreadNode，代表被追踪的线程
     */
    private TraceNode root;

    /**
     * 当前节点指针
     * 指向当前正在执行的节点，用于在方法调用过程中动态维护树结构
     */
    private TraceNode current;

    /**
     * 节点计数器
     * 记录树中节点的总数，用于统计和调试
     */
    private int nodeCount = 0;

    /**
     * 构造函数
     *
     * 创建一个新的TraceTree，使用指定的ThreadNode作为根节点，
     * 并将当前节点指针指向根节点
     *
     * @param root 根节点，通常是ThreadNode对象
     */
    public TraceTree(ThreadNode root) {
        this.root = root;
        this.current = root;
    }

    /**
     * 开始一个新的方法调用
     *
     * 当追踪到一个方法调用时，调用此方法创建或找到对应的方法节点，
     * 并将其设置为当前节点。如果在当前节点的子节点中已经存在相同的方法节点，
     * 则复用该节点；否则创建新的方法节点。
     *
     * @param className 方法所属的类名
     * @param methodName 方法名称
     * @param lineNumber 方法调用的行号
     * @param isInvoking 是否在其他类中调用该方法
     */
    public void begin(String className, String methodName, int lineNumber, boolean isInvoking) {
        // 在当前节点的子节点中查找是否已存在该方法节点
        TraceNode child = findChild(current, className, methodName, lineNumber);
        if (child == null) {
            // 如果不存在，创建新的方法节点
            child = new MethodNode(className, methodName, lineNumber, isInvoking);
            // 将新节点添加为当前节点的子节点
            current.addChild(child);
        }
        // 调用节点的begin方法，触发节点的开始逻辑
        child.begin();
        // 将当前节点指针移动到该节点
        current = child;
        // 节点计数器递增
        nodeCount += 1;
    }

    /**
     * 在指定节点的子节点中查找匹配的方法节点
     *
     * 根据类名、方法名和行号在子节点列表中查找是否存在匹配的节点
     *
     * @param node 要查找的父节点
     * @param className 目标类名
     * @param methodName 目标方法名
     * @param lineNumber 目标行号
     * @return 匹配的节点，如果未找到则返回null
     */
    private TraceNode findChild(TraceNode node, String className, String methodName, int lineNumber) {
        List<TraceNode> childList = node.getChildren();
        if (childList != null) {
            // 使用索引遍历而不是foreach/iterator，以减少内存消耗
            for (int i = 0; i < childList.size(); i++) {
                TraceNode child = childList.get(i);
                // 检查子节点是否匹配
                if (matchNode(child, className, methodName, lineNumber)) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * 判断节点是否匹配指定的类名、方法名和行号
     *
     * @param node 要判断的节点
     * @param className 目标类名
     * @param methodName 目标方法名
     * @param lineNumber 目标行号
     * @return 如果节点匹配则返回true，否则返回false
     */
    private boolean matchNode(TraceNode node, String className, String methodName, int lineNumber) {
        // 只有MethodNode类型的节点才进行匹配
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            // 首先检查行号是否匹配
            if (lineNumber != methodNode.getLineNumber()) return false;
            // 检查类名是否匹配
            if (className != null ? !className.equals(methodNode.getClassName()) : methodNode.getClassName() != null) return false;
            // 检查方法名是否匹配
            return methodName != null ? methodName.equals(methodNode.getMethodName()) : methodNode.getMethodName() == null;
        }
        return false;
    }

    /**
     * 结束当前节点
     *
     * 调用当前节点的end方法，然后将当前节点指针移动到父节点。
     * 这表示当前方法的执行已经结束，返回到调用者。
     */
    public void end() {
        // 调用当前节点的结束方法
        current.end();
        // 将当前节点指针移动到父节点
        if (current.parent() != null) {
            // TODO 为什么会到达这里？ 调用end次数比begin多？
            current = current.parent();
        }
    }

    /**
     * 结束当前节点并记录异常信息
     *
     * 当方法执行过程中抛出异常时调用此方法，创建一个ThrowNode来记录异常信息，
     * 并将其添加为当前节点的子节点，然后结束当前节点。
     *
     * @param throwable 抛出的异常对象
     * @paramLineNumber 异常发生的行号
     */
    public void end(Throwable throwable, int lineNumber) {
        // 创建异常节点
        ThrowNode throwNode = new ThrowNode();
        // 设置异常类型
        throwNode.setException(throwable.getClass().getName());
        // 设置异常消息
        throwNode.setMessage(throwable.getMessage());
        // 设置异常发生的行号
        throwNode.setLineNumber(lineNumber);
        // 将异常节点添加为当前节点的子节点
        current.addChild(throwNode);
        // 结束当前节点，标记为抛出异常
        this.end(true);
    }

    /**
     * 结束当前节点（带异常标记）
     *
     * 如果isThrow为true，则在当前节点上标记异常信息，
     * 对于MethodNode类型的节点，还会将其throw属性设置为true
     *
     * @param isThrow 是否抛出异常
     */
    public void end(boolean isThrow) {
        if (isThrow) {
            // 设置节点备注为异常信息
            current.setMark("throws Exception");
            // 如果当前节点是MethodNode类型，标记其throw属性
            if (current instanceof MethodNode) {
                MethodNode methodNode = (MethodNode) current;
                methodNode.setThrow(true);
            }
        }
        // 调用普通的end方法结束当前节点
        this.end();
    }

    /**
     * 修整树节点
     *
     * 对树中的所有节点进行后处理，例如规范化类名等操作。
     * 通常在trace追踪完成后调用，以确保数据的一致性和规范性。
     */
    public void trim() {
        // 规范化所有节点的类名
        this.normalizeClassName(root);
    }

    /**
     * 递归规范化节点的类名
     *
     * 将节点中的类名转换为标准格式，放在trace结束后统一转换，
     * 以减少追踪过程中的重复操作，提高性能。
     *
     * @param node 要处理的节点
     */
    private void normalizeClassName(TraceNode node) {
        // 如果是MethodNode类型的节点，规范化其类名
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            // 获取节点的原始类名
            String nodeClassName = methodNode.getClassName();
            // 调用工具类规范化类名（例如将基本类型转换为标准格式）
            String normalizeClassName = StringUtils.normalizeClassName(nodeClassName);
            // 设置规范化后的类名
            methodNode.setClassName(normalizeClassName);
        }
        // 递归处理所有子节点
        List<TraceNode> children = node.getChildren();
        if (children != null) {
            // 使用索引遍历而不是foreach，以减少内存碎片
            for (int i = 0; i < children.size(); i++) {
                TraceNode child = children.get(i);
                normalizeClassName(child);
            }
        }
    }

    /**
     * 获取树的根节点
     *
     * @return 根节点对象
     */
    public TraceNode getRoot() {
        return root;
    }

    /**
     * 获取当前节点
     *
     * @return 当前正在执行的节点对象
     */
    public TraceNode current() {
        return current;
    }

    /**
     * 获取节点总数
     *
     * @return 树中节点的总数
     */
    public int getNodeCount() {
        return nodeCount;
    }
}
