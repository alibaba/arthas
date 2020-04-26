package com.taobao.arthas.core.command.model;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/**
 * @author gongdewei 2020/4/26
 */
public class ThreadInfoExt {

    private ThreadInfo threadInfo;

    private long cpuUsage;

    public ThreadInfoExt(ThreadInfo threadInfo, long cpuUsage) {
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

    public long getThreadId() {
        return threadInfo.getThreadId();
    }

    public String getThreadName() {
        return threadInfo.getThreadName();
    }

    public Thread.State getThreadState() {
        return threadInfo.getThreadState();
    }

    public long getBlockedTime() {
        return threadInfo.getBlockedTime();
    }

    public long getBlockedCount() {
        return threadInfo.getBlockedCount();
    }

    public long getWaitedTime() {
        return threadInfo.getWaitedTime();
    }

    public long getWaitedCount() {
        return threadInfo.getWaitedCount();
    }

    public LockInfo getLockInfo() {
        return threadInfo.getLockInfo();
    }

    public String getLockName() {
        return threadInfo.getLockName();
    }

    public long getLockOwnerId() {
        return threadInfo.getLockOwnerId();
    }

    public String getLockOwnerName() {
        return threadInfo.getLockOwnerName();
    }

    public StackTraceElement[] getStackTrace() {
        return threadInfo.getStackTrace();
    }

    public boolean isSuspended() {
        return threadInfo.isSuspended();
    }

    public boolean isInNative() {
        return threadInfo.isInNative();
    }

    public MonitorInfo[] getLockedMonitors() {
        return threadInfo.getLockedMonitors();
    }

    public LockInfo[] getLockedSynchronizers() {
        return threadInfo.getLockedSynchronizers();
    }
}
