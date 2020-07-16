package com.taobao.arthas.core.command.model;

import java.lang.management.ThreadInfo;

/**
 * ThreadInfo with cpuUsage
 *
 * @author gongdewei 2020/4/26
 */
public class ThreadCpuInfo {

    private ThreadInfo threadInfo;

    private long cpuUsage;

    public ThreadCpuInfo(ThreadInfo threadInfo, long cpuUsage) {
        this.threadInfo = threadInfo;
        this.cpuUsage = cpuUsage;
    }

    public long getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(long cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public ThreadInfo threadInfo() {
        return threadInfo;
    }

    public void setThreadInfo(ThreadInfo threadInfo) {
        this.threadInfo = threadInfo;
    }
}
