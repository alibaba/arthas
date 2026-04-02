package com.taobao.arthas.core.command.model;

/**
 * TraceCommand的方法调用节点
 *
 * 用于在方法调用链追踪中表示一个具体的方法调用节点，
 * 记录方法的调用信息、耗时统计和异常情况
 *
 * @author gongdewei 2020/4/29
 */
public class MethodNode extends TraceNode {

    /**
     * 方法所属的类名
     */
    private String className;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 方法调用所在的行号
     */
    private int lineNumber;

    /**
     * 方法执行过程中是否抛出异常
     * true表示抛出异常，false或null表示正常执行
     */
    private Boolean isThrow;

    /**
     * 异常信息字符串
     * 当isThrow为true时，记录抛出的异常信息
     */
    private String throwExp;

    /**
     * 是否为invoke方法调用
     * true表示beforeInvoke（在方法调用之前），false表示方法体入口的onBefore（在方法体内部）
     */
    private boolean isInvoking;

    /**
     * 方法开始执行的时间戳（纳秒）
     */
    private long beginTimestamp;

    /**
     * 方法结束执行的时间戳（纳秒）
     */
    private long endTimestamp;

    /**
     * 合并统计相同调用的最小耗时
     * 初始值为Long.MAX_VALUE，每次调用后会更新为最小值
     */
    private long minCost = Long.MAX_VALUE;

    /**
     * 合并统计相同调用的最大耗时
     * 初始值为Long.MIN_VALUE，每次调用后会更新为最大值
     */
    private long maxCost = Long.MIN_VALUE;

    /**
     * 合并统计相同调用的总耗时
     */
    private long totalCost = 0;

    /**
     * 相同调用的次数统计
     */
    private long times = 0;


    /**
     * 构造函数
     *
     * @param className 方法所属的类名
     * @param methodName 方法名称
     * @param lineNumber 方法调用所在的行号
     * @param isInvoking 是否为invoke方法调用
     */
    public MethodNode(String className, String methodName, int lineNumber, boolean isInvoking) {
        // 调用父类TraceNode的构造函数，设置节点类型为"method"
        super("method");
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.isInvoking = isInvoking;
    }

    /**
     * 开始计时
     * 记录方法开始执行的时间戳，使用System.nanoTime()获取高精度时间
     */
    public void begin() {
        // 使用纳秒级时间戳确保精度
        beginTimestamp = System.nanoTime();
    }

    /**
     * 结束计时并统计耗时
     * 记录方法结束执行的时间戳，并更新最小、最大、总耗时和调用次数
     */
    public void end() {
        // 记录结束时间戳
        endTimestamp = System.nanoTime();

        // 计算本次调用的耗时
        long cost = getCost();

        // 更新最小耗时：如果本次耗时小于当前最小值，则更新
        if (cost < minCost) {
            minCost = cost;
        }

        // 更新最大耗时：如果本次耗时大于当前最大值，则更新
        if (cost > maxCost) {
            maxCost = cost;
        }

        // 调用次数加1
        times++;

        // 累加到总耗时
        totalCost += cost;
    }

    /**
     * 计算最后一次方法调用的耗时
     *
     * @return 方法执行耗时（纳秒）
     */
    public long getCost() {
        // 返回结束时间戳与开始时间戳的差值
        return endTimestamp - beginTimestamp;
    }

    /**
     * 获取方法所属的类名
     *
     * @return 类名字符串
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置方法所属的类名
     *
     * @param className 类名字符串
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 获取方法名称
     *
     * @return 方法名字符串
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * 设置方法名称
     *
     * @param methodName 方法名字符串
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * 获取方法调用所在的行号
     *
     * @return 行号
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * 设置方法调用所在的行号
     *
     * @param lineNumber 行号
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * 获取方法执行过程中是否抛出异常
     *
     * @return true表示抛出异常，false或null表示正常执行
     */
    public Boolean getThrow() {
        return isThrow;
    }

    /**
     * 设置方法执行过程中是否抛出异常
     *
     * @param aThrow true表示抛出异常，false表示正常执行
     */
    public void setThrow(Boolean aThrow) {
        isThrow = aThrow;
    }

    /**
     * 获取异常信息字符串
     *
     * @return 异常信息，如果未抛出异常则可能为null
     */
    public String getThrowExp() {
        return throwExp;
    }

    /**
     * 设置异常信息字符串
     *
     * @param throwExp 异常信息字符串
     */
    public void setThrowExp(String throwExp) {
        this.throwExp = throwExp;
    }

    /**
     * 获取最小耗时
     *
     * @return 所有调用中的最小耗时（纳秒）
     */
    public long getMinCost() {
        return minCost;
    }

    /**
     * 设置最小耗时
     *
     * @param minCost 最小耗时（纳秒）
     */
    public void setMinCost(long minCost) {
        this.minCost = minCost;
    }

    /**
     * 获取最大耗时
     *
     * @return 所有调用中的最大耗时（纳秒）
     */
    public long getMaxCost() {
        return maxCost;
    }

    /**
     * 设置最大耗时
     *
     * @param maxCost 最大耗时（纳秒）
     */
    public void setMaxCost(long maxCost) {
        this.maxCost = maxCost;
    }

    /**
     * 获取总耗时
     *
     * @return 所有调用的总耗时（纳秒）
     */
    public long getTotalCost() {
        return totalCost;
    }

    /**
     * 设置总耗时
     *
     * @param totalCost 总耗时（纳秒）
     */
    public void setTotalCost(long totalCost) {
        this.totalCost = totalCost;
    }

    /**
     * 获取调用次数
     *
     * @return 方法被调用的次数
     */
    public long getTimes() {
        return times;
    }

    /**
     * 设置调用次数
     *
     * @param times 调用次数
     */
    public void setTimes(long times) {
        this.times = times;
    }

    /**
     * 判断是否为invoke方法调用
     *
     * @return true表示beforeInvoke，false表示方法体入口的onBefore
     */
    public boolean isInvoking() {
        return isInvoking;
    }

    /**
     * 设置是否为invoke方法调用
     *
     * @param invoking true表示beforeInvoke，false表示方法体入口的onBefore
     */
    public void setInvoking(boolean invoking) {
        isInvoking = invoking;
    }
}
