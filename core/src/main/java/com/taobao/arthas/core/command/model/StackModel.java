package com.taobao.arthas.core.command.model;

import java.util.Date;

/**
 * StackCommand result model
 * @author gongdewei 2020/4/13
 */
public class StackModel extends ResultModel {

    private Date ts;
    private double cost;
    private String traceId;
    private String rpcId;
    private String threadName;
    private String threadId;
    private boolean daemon;
    private int priority;
    /* Thread Current ClassLoader */
    private String classloader;
    private StackTraceElement[] stackTrace;

    @Override
    public String getType() {
        return "stack";
    }

    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
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

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }
}
