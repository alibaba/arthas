package com.taobao.arthas.core.command.model;

/**
 * 运行时信息视图对象
 * 用于封装和展示 JVM 运行时相关的系统信息，包括操作系统、Java 环境、系统负载等
 *
 * Dashboard - Runtime
 *
 * @author gongdewei 2020/4/22
 */
public class RuntimeInfoVO {

    /**
     * 操作系统名称，例如 "Linux"、"Mac OS X" 等
     */
    private String osName;

    /**
     * 操作系统版本，例如 "5.4.0-42-generic" 等
     */
    private String osVersion;

    /**
     * Java 版本，例如 "1.8.0_281" 等
     */
    private String javaVersion;

    /**
     * Java 安装目录路径
     */
    private String javaHome;

    /**
     * 系统平均负载
     * 表示最近一段时间内运行队列中的平均进程数
     */
    private double systemLoadAverage;

    /**
     * 处理器数量（CPU 核心数）
     */
    private int processors;

    /**
     * JVM 运行时长（单位：毫秒）
     * 表示 JVM 启动到现在的时间
     */
    private long uptime;

    /**
     * 时间戳
     * 表示该运行时信息被采集的时间点
     */
    private long timestamp;

    /**
     * 默认构造函数
     * 创建一个空的运行时信息视图对象
     */
    public RuntimeInfoVO() {
    }

    /**
     * 获取操作系统名称
     *
     * @return 操作系统名称
     */
    public String getOsName() {
        return osName;
    }

    /**
     * 设置操作系统名称
     *
     * @param osName 操作系统名称
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * 获取操作系统版本
     *
     * @return 操作系统版本
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * 设置操作系统版本
     *
     * @param osVersion 操作系统版本
     */
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * 获取 Java 版本
     *
     * @return Java 版本
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * 设置 Java 版本
     *
     * @param javaVersion Java 版本
     */
    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    /**
     * 获取 Java 安装目录路径
     *
     * @return Java 安装目录路径
     */
    public String getJavaHome() {
        return javaHome;
    }

    /**
     * 设置 Java 安装目录路径
     *
     * @param javaHome Java 安装目录路径
     */
    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    /**
     * 获取系统平均负载
     *
     * @return 系统平均负载值
     */
    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }

    /**
     * 设置系统平均负载
     *
     * @param systemLoadAverage 系统平均负载值
     */
    public void setSystemLoadAverage(double systemLoadAverage) {
        this.systemLoadAverage = systemLoadAverage;
    }

    /**
     * 获取处理器数量
     *
     * @return 处理器数量（CPU 核心数）
     */
    public int getProcessors() {
        return processors;
    }

    /**
     * 设置处理器数量
     *
     * @param processors 处理器数量（CPU 核心数）
     */
    public void setProcessors(int processors) {
        this.processors = processors;
    }

    /**
     * 获取 JVM 运行时长
     *
     * @return JVM 运行时长（单位：毫秒）
     */
    public long getUptime() {
        return uptime;
    }

    /**
     * 设置 JVM 运行时长
     *
     * @param uptime JVM 运行时长（单位：毫秒）
     */
    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 设置时间戳
     *
     * @param timestamp 时间戳
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
