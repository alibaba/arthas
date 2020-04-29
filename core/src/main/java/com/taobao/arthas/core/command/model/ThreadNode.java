package com.taobao.arthas.core.command.model;

import java.util.Date;

/**
 * @author gongdewei 2020/4/29
 */
public class ThreadNode extends TraceNode {

    private String threadName;
    private String threadId;
    private boolean daemon;
    private int priority;
    private String classloader;
    private Date timestamp;

    private String traceId;
    private String rpcId;

    public ThreadNode() {
        timestamp = new Date();
    }

    public ThreadNode(String threadName, String threadId, boolean daemon, int priority, String classloader) {
        this.threadName = threadName;
        this.threadId = threadId;
        this.daemon = daemon;
        this.priority = priority;
        this.classloader = classloader;
        timestamp = new Date();
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
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
