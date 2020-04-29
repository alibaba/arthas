package com.taobao.arthas.core.command.model;


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
        Integer nodeId = getNodeId(className, methodName, -1);
        TraceNode child = current.findChild(nodeId);
        if (child == null) {
            child = new MethodNode(nodeId, className, methodName, -1, false);
            current.addChild(child);
        }
        child.begin();
        current = child;
    }

    public void begin(String className, String methodName, int lineNumber) {
        Integer nodeId = getNodeId(className, methodName, lineNumber);
        TraceNode child = current.findChild(nodeId);
        if (child == null) {
            child = new MethodNode(nodeId, className, methodName, lineNumber, true);
            current.addChild(child);
        }
        child.begin();
        current = child;
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

    public TraceNode getRoot() {
        return root;
    }

    public TraceNode current() {
        return current;
    }

}
