package com.taobao.arthas.core.command.model;

/**
 * Method call node of TraceCommand
 * @author gongdewei 2020/4/29
 */
public class MethodNode extends TraceNode {

    private String className;
    private String methodName;
    private int lineNumber;
    private Boolean isThrow;
    private String throwExp;

    /**
     * 是否为invoke方法，true为beforeInvoke，false为方法体入口的onBefore
     */
    private boolean isInvoking;

    /**
     * 开始时间戳
     */
    private long beginTimestamp;

    /**
     * 结束时间戳
     */
    private long endTimestamp;

    /**
     * 合并统计相同调用,并计算最小\最大\总耗时
     */
    private long minCost = Long.MAX_VALUE;
    private long maxCost = Long.MIN_VALUE;
    private long totalCost = 0;
    private long times = 0;


    public MethodNode(String className, String methodName, int lineNumber, boolean isInvoking) {
        super("method");
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.isInvoking = isInvoking;
    }

    public void begin() {
        beginTimestamp = System.nanoTime();
    }

    public void end() {
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
    }

    public long getCost() {
        return endTimestamp - beginTimestamp;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Boolean getThrow() {
        return isThrow;
    }

    public void setThrow(Boolean aThrow) {
        isThrow = aThrow;
    }

    public String getThrowExp() {
        return throwExp;
    }

    public void setThrowExp(String throwExp) {
        this.throwExp = throwExp;
    }

    public long getMinCost() {
        return minCost;
    }

    public void setMinCost(long minCost) {
        this.minCost = minCost;
    }

    public long getMaxCost() {
        return maxCost;
    }

    public void setMaxCost(long maxCost) {
        this.maxCost = maxCost;
    }

    public long getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(long totalCost) {
        this.totalCost = totalCost;
    }

    public long getTimes() {
        return times;
    }

    public void setTimes(long times) {
        this.times = times;
    }

    public boolean isInvoking() {
        return isInvoking;
    }

    public void setInvoking(boolean invoking) {
        isInvoking = invoking;
    }
}
