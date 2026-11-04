package com.taobao.arthas.core.command.monitor200;

import java.time.LocalDateTime;

/**
 * 数据监控用的value for MonitorCommand
 *
 * @author vlinux
 */
public class MonitorData {
    private String className;
    private String methodName;
    private int total;
    private int success;
    private int failed;
    private double cost;
    private LocalDateTime timestamp;

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

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public LocalDateTime getTimestamp() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
