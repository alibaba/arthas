package com.taobao.arthas.core.command.model;

import java.time.LocalDateTime;

/**
 * Thread root node of TraceCommand
 * @author gongdewei 2020/4/29
 */
public class ThreadNode extends TraceNode {

    private String threadName;
    private long threadId;
    private boolean daemon;
    private int priority;
    private String classloader;
    private LocalDateTime timestamp;

    private String traceId;
    private String rpcId;

    public ThreadNode() {
        super("thread");
        timestamp = LocalDateTime.now();
    }

    public ThreadNode(String threadName, long threadId, boolean daemon, int priority, String classloader) {
        super("thread");
        this.threadName = threadName;
        this.threadId = threadId;
        this.daemon = daemon;
        this.priority = priority;
        this.classloader = classloader;
        timestamp = LocalDateTime.now();
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getClassloader() {
        return classloader;
    }

    public void setClassloader(String classloader) {
        this.classloader = classloader;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRpcId() {
        return rpcId;
    }

    public void setRpcId(String rpcId) {
        this.rpcId = rpcId;
    }
}
