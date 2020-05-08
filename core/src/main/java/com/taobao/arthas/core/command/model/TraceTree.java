package com.taobao.arthas.core.command.model;


import com.taobao.arthas.core.util.StringUtils;

import java.util.List;

/**
 * @author gongdewei 2020/4/28
 */
public class TraceTree {
    private TraceNode root;

    private TraceNode current;

    public TraceTree(ThreadNode root) {
        this.root = root;
        this.current = root;
    }

    public void begin(String className, String methodName) {
        //non-invoking
        begin(className, methodName, -1, false);
    }

    public void begin(String className, String methodName, int lineNumber) {
        //invoking
        begin(className, methodName, lineNumber, true);
    }

    private void begin(String className, String methodName, int lineNumber, boolean isInvoking) {
        //Integer nodeId = getNodeId(className, methodName, lineNumber);
        TraceNode child = findChild(current, className, methodName, lineNumber);
        if (child == null) {
            child = new MethodNode(className, methodName, lineNumber, isInvoking);
            current.addChild(child);
        }
        child.begin();
        current = child;
    }

    private TraceNode findChild(TraceNode node, String className, String methodName, int lineNumber) {
        List<TraceNode> childList = node.getChildren();
        if (childList != null) {
            for (TraceNode child : childList) {
                if (matchNode(child, className, methodName, lineNumber)) {
                    return child;
                }
            }
        }
        return null;
    }

    private boolean matchNode(TraceNode node, String className, String methodName, int lineNumber) {
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            if (lineNumber != methodNode.getLineNumber()) return false;
            if (className != null ? !className.equals(methodNode.getClassName()) : methodNode.getClassName() != null) return false;
            return methodName != null ? methodName.equals(methodNode.getMethodName()) : methodNode.getMethodName() == null;
        }
        return false;
    }

    public void end() {
        current.end();
        current = current.parent();
    }

    public void end(String exceptionClassName, int lineNumber) {
        if (current instanceof MethodNode) {
            MethodNode currentNode = (MethodNode) current;
            currentNode.setThrow(true);
            currentNode.setThrowExp(exceptionClassName);
            currentNode.setLineNumber(lineNumber);
        }
        current.setMark("throw:"+exceptionClassName);
        this.end();
    }

    public void end(boolean isThrow) {
        if (isThrow) {
            current.setMark("throws Exception");
            if (current instanceof MethodNode) {
                MethodNode methodNode = (MethodNode) current;
                methodNode.setThrow(true);
            }
        }
        this.end();
    }

    private int getNodeId(String className, String methodName, int lineNumber) {
        //from Arrays.hashCode(Object a[])
        //memory optimizing: avoid create new object[]
        int result = 1;
        result = 31 * result + className.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + lineNumber;
        return result;
    }

    public void normalizeClassName(TraceNode node) {
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            methodNode.setClassName(StringUtils.normalizeClassName(methodNode.getClassName()));
        }
        List<TraceNode> children = node.getChildren();
        if (children != null) {
            for (TraceNode child : children) {
                normalizeClassName(child);
            }
        }
    }

    public TraceNode getRoot() {
        return root;
    }

    public TraceNode current() {
        return current;
    }

}
