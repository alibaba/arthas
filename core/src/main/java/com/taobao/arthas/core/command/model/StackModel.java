package com.taobao.arthas.core.command.model;

import java.time.LocalDateTime;

/**
 * 堆栈命令的结果模型
 * <p>
 * 该类用于封装线程堆栈追踪信息的执行结果，包含线程的详细信息和堆栈轨迹
 * </p>
 *
 * @author gongdewei 2020/4/13
 */
public class StackModel extends ResultModel {

    /**
     * 时间戳
     * <p>
     * 记录获取堆栈信息的时间点
     * </p>
     */
    private LocalDateTime ts;

    /**
     * 执行耗时
     * <p>
     * 获取堆栈信息所消耗的时间（单位：毫秒）
     * </p>
     */
    private double cost;

    /**
     * 追踪ID
     * <p>
     * 用于分布式链路追踪的唯一标识符
     * </p>
     */
    private String traceId;

    /**
     * RPC ID
     * <p>
     * 用于标识RPC调用链路中的节点
     * </p>
     */
    private String rpcId;

    /**
     * 线程名称
     * <p>
     * 被追踪线程的名称
     * </p>
     */
    private String threadName;

    /**
     * 线程ID
     * <p>
     * 被追踪线程的唯一标识符
     * </p>
     */
    private String threadId;

    /**
     * 是否为守护线程
     * <p>
     * true表示守护线程，false表示用户线程
     * </p>
     */
    private boolean daemon;

    /**
     * 线程优先级
     * <p>
     * 线程的优先级级别（1-10，默认为5）
     * </p>
     */
    private int priority;

    /**
     * 当前线程的类加载器
     * <p>
     * 线程当前使用的类加载器的字符串表示
     * </p>
     */
    private String classloader;

    /**
     * 堆栈轨迹数组
     * <p>
     * 包含线程调用栈中的所有堆栈帧信息
     * </p>
     */
    private StackTraceElement[] stackTrace;

    /**
     * 获取结果类型
     * <p>
     * 返回"stack"标识这是一个堆栈命令的结果
     * </p>
     *
     * @return 结果类型字符串"stack"
     */
    @Override
    public String getType() {
        return "stack";
    }

    /**
     * 获取时间戳
     *
     * @return 获取堆栈信息的时间点
     */
    public LocalDateTime getTs() {
        return ts;
    }

    /**
     * 设置时间戳
     *
     * @param ts 获取堆栈信息的时间点
     */
    public void setTs(LocalDateTime ts) {
        this.ts = ts;
    }

    /**
     * 获取执行耗时
     *
     * @return 获取堆栈信息所消耗的时间（毫秒）
     */
    public double getCost() {
        return cost;
    }

    /**
     * 设置执行耗时
     *
     * @param cost 获取堆栈信息所消耗的时间（毫秒）
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * 获取线程名称
     *
     * @return 线程名称
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * 设置线程名称
     *
     * @param threadName 线程名称
     */
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     * 获取线程ID
     *
     * @return 线程ID
     */
    public String getThreadId() {
        return threadId;
    }

    /**
     * 设置线程ID
     *
     * @param threadId 线程ID
     */
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    /**
     * 判断是否为守护线程
     *
     * @return true表示守护线程，false表示用户线程
     */
    public boolean isDaemon() {
        return daemon;
    }

    /**
     * 设置是否为守护线程
     *
     * @param daemon true表示守护线程，false表示用户线程
     */
    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * 获取线程优先级
     *
     * @return 线程优先级（1-10）
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 设置线程优先级
     *
     * @param priority 线程优先级（1-10）
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * 获取类加载器
     *
     * @return 线程当前使用的类加载器
     */
    public String getClassloader() {
        return classloader;
    }

    /**
     * 设置类加载器
     *
     * @param classloader 线程当前使用的类加载器
     */
    public void setClassloader(String classloader) {
        this.classloader = classloader;
    }

    /**
     * 获取追踪ID
     *
     * @return 分布式链路追踪的唯一标识符
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * 设置追踪ID
     *
     * @param traceId 分布式链路追踪的唯一标识符
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * 获取RPC ID
     *
     * @return RPC调用链路中的节点标识
     */
    public String getRpcId() {
        return rpcId;
    }

    /**
     * 设置RPC ID
     *
     * @param rpcId RPC调用链路中的节点标识
     */
    public void setRpcId(String rpcId) {
        this.rpcId = rpcId;
    }

    /**
     * 获取堆栈轨迹数组
     *
     * @return 包含所有堆栈帧信息的数组
     */
    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    /**
     * 设置堆栈轨迹数组
     *
     * @param stackTrace 包含所有堆栈帧信息的数组
     */
    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }
}
