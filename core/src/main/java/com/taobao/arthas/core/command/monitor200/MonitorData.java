package com.taobao.arthas.core.command.monitor200;

import java.time.LocalDateTime;

/**
 * 数据监控用的value for MonitorCommand
 * 用于 MonitorCommand 的数据监控值对象
 *
 * @author vlinux
 */
public class MonitorData {
    // 类名
    private String className;
    // 方法名
    private String methodName;
    // 总调用次数
    private int total;
    // 成功调用次数
    private int success;
    // 失败调用次数
    private int failed;
    // 耗时（单位：毫秒）
    private double cost;
    // 时间戳
    private LocalDateTime timestamp;

    /**
     * 获取类名
     * @return 类名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置类名
     * @param className 类名
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 获取方法名
     * @return 方法名
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * 设置方法名
     * @param methodName 方法名
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * 获取总调用次数
     * @return 总调用次数
     */
    public int getTotal() {
        return total;
    }

    /**
     * 设置总调用次数
     * @param total 总调用次数
     */
    public void setTotal(int total) {
        this.total = total;
    }

    /**
     * 获取成功调用次数
     * @return 成功调用次数
     */
    public int getSuccess() {
        return success;
    }

    /**
     * 设置成功调用次数
     * @param success 成功调用次数
     */
    public void setSuccess(int success) {
        this.success = success;
    }

    /**
     * 获取失败调用次数
     * @return 失败调用次数
     */
    public int getFailed() {
        return failed;
    }

    /**
     * 设置失败调用次数
     * @param failed 失败调用次数
     */
    public void setFailed(int failed) {
        this.failed = failed;
    }

    /**
     * 获取耗时
     * @return 耗时（单位：毫秒）
     */
    public double getCost() {
        return cost;
    }

    /**
     * 设置耗时
     * @param cost 耗时（单位：毫秒）
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * 获取时间戳
     * 如果时间戳为空，则使用当前时间
     * @return 时间戳
     */
    public LocalDateTime getTimestamp() {
        // 如果时间戳为空，初始化为当前时间
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        return timestamp;
    }

    /**
     * 设置时间戳
     * @param timestamp 时间戳
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
