package com.taobao.arthas.core.command.model;

/**
 * @author jie xu
 * @description
 * @created 2021/10/14
 **/
public class SyncNode extends TraceNode {

    private String className;
    private String methodName;
    private int lineNumber;
    private long beginTimestamp;
    private long endTimestamp;
    private long cost;
    public SyncNode(String className, String methodName, int lineNumber) {
        super("sync");
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
    }

    public void begin() {
        this.beginTimestamp = System.nanoTime();
    }

    public void end() {
        this.endTimestamp = System.nanoTime();
        this.cost = this.endTimestamp - this.beginTimestamp;
    }

    public long getCost() {
        return this.cost;
    }

    public String getClassName() {
        return this.className;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }
}
