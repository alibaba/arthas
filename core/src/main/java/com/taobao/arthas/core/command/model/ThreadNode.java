package com.taobao.arthas.core.command.model;

import java.time.LocalDateTime;

/**
 * TraceCommand的线程根节点
 * 用于在方法调用链路追踪中表示线程信息，包含线程的基本属性和分布式追踪相关信息
 *
 * @author gongdewei 2020/4/29
 */
public class ThreadNode extends TraceNode {

    // 线程名称
    private String threadName;
    // 线程ID
    private long threadId;
    // 是否为守护线程
    private boolean daemon;
    // 线程优先级
    private int priority;
    // 类加载器信息
    private String classloader;
    // 时间戳，记录节点创建时间
    private LocalDateTime timestamp;

    // 分布式追踪ID，用于关联同一个请求的多个调用链路
    private String traceId;
    // RPC ID，用于标识分布式调用中的父子关系
    private String rpcId;

    /**
     * 默认构造函数
     * 自动设置节点类型为"thread"并记录当前时间戳
     */
    public ThreadNode() {
        super("thread");
        timestamp = LocalDateTime.now();
    }

    /**
     * 完整构造函数
     * @param threadName 线程名称
     * @param threadId 线程ID
     * @param daemon 是否为守护线程
     * @param priority 线程优先级
     * @param classloader 类加载器信息
     */
    public ThreadNode(String threadName, long threadId, boolean daemon, int priority, String classloader) {
        super("thread");
        this.threadName = threadName;
        this.threadId = threadId;
        this.daemon = daemon;
        this.priority = priority;
        this.classloader = classloader;
        timestamp = LocalDateTime.now();
    }

    /**
     * 获取线程名称
     * @return 线程名称
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * 设置线程名称
     * @param threadName 线程名称
     */
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     * 获取线程ID
     * @return 线程ID
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * 设置线程ID
     * @param threadId 线程ID
     */
    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    /**
     * 判断是否为守护线程
     * @return true表示是守护线程，false表示不是
     */
    public boolean isDaemon() {
        return daemon;
    }

    /**
     * 设置是否为守护线程
     * @param daemon true表示是守护线程，false表示不是
     */
    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * 获取线程优先级
     * @return 线程优先级，范围1-10，数值越大优先级越高
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 设置线程优先级
     * @param priority 线程优先级，范围1-10，数值越大优先级越高
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * 获取类加载器信息
     * @return 类加载器的字符串表示
     */
    public String getClassloader() {
        return classloader;
    }

    /**
     * 设置类加载器信息
     * @param classloader 类加载器的字符串表示
     */
    public void setClassloader(String classloader) {
        this.classloader = classloader;
    }

    /**
     * 获取节点创建时间戳
     * @return 节点创建的时间
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 设置节点创建时间戳
     * @param timestamp 节点创建的时间
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 获取分布式追踪ID
     * @return 追踪ID字符串
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * 设置分布式追踪ID
     * @param traceId 追踪ID字符串
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * 获取RPC ID
     * @return RPC ID字符串
     */
    public String getRpcId() {
        return rpcId;
    }

    /**
     * 设置RPC ID
     * @param rpcId RPC ID字符串
     */
    public void setRpcId(String rpcId) {
        this.rpcId = rpcId;
    }
}
