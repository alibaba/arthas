package com.taobao.arthas.core.command.model;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/**
 * Busy thread info, include ThreadInfo fields
 *
 * @author gongdewei 2020/4/26
 */
public class BusyThreadInfo extends ThreadVO {

    private long         blockedTime;
    private long         blockedCount;
    private long         waitedTime;
    private long         waitedCount;
    private LockInfo lockInfo;
    private String       lockName;
    private long         lockOwnerId;
    private String       lockOwnerName;
    private boolean      inNative;
    private boolean      suspended;
    private StackTraceElement[] stackTrace;
    private MonitorInfo[]       lockedMonitors;
    private LockInfo[]          lockedSynchronizers;


    public BusyThreadInfo(ThreadVO thread, ThreadInfo threadInfo) {
        this.setId(thread.getId());
        this.setName(thread.getName());
        this.setDaemon(thread.isDaemon());
        this.setInterrupted(thread.isInterrupted());
        this.setPriority(thread.getPriority());
        this.setGroup(thread.getGroup());
        this.setState(thread.getState());
        this.setCpu(thread.getCpu());
        this.setDeltaTime(thread.getDeltaTime());
        this.setTime(thread.getTime());

        //thread info
        if (threadInfo != null) {
            this.setLockInfo(threadInfo.getLockInfo());
            this.setLockedMonitors(threadInfo.getLockedMonitors());
            this.setLockedSynchronizers(threadInfo.getLockedSynchronizers());
            this.setLockName(threadInfo.getLockName());
            this.setLockOwnerId(threadInfo.getLockOwnerId());
            this.setLockOwnerName(threadInfo.getLockOwnerName());
            this.setStackTrace(threadInfo.getStackTrace());
            this.setBlockedCount(threadInfo.getBlockedCount());
            this.setBlockedTime(threadInfo.getBlockedTime());
            this.setInNative(threadInfo.isInNative());
            this.setSuspended(threadInfo.isSuspended());
            this.setWaitedCount(threadInfo.getWaitedCount());
            this.setWaitedTime(threadInfo.getWaitedTime());
        }

    }

    public long getBlockedTime() {
        return blockedTime;
    }

    public void setBlockedTime(long blockedTime) {
        this.blockedTime = blockedTime;
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }

    public long getWaitedTime() {
        return waitedTime;
    }

    public void setWaitedTime(long waitedTime) {
        this.waitedTime = waitedTime;
    }

    public long getWaitedCount() {
        return waitedCount;
    }

    public void setWaitedCount(long waitedCount) {
        this.waitedCount = waitedCount;
    }

    public LockInfo getLockInfo() {
        return lockInfo;
    }

    public void setLockInfo(LockInfo lockInfo) {
        this.lockInfo = lockInfo;
    }

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public long getLockOwnerId() {
        return lockOwnerId;
    }

    public void setLockOwnerId(long lockOwnerId) {
        this.lockOwnerId = lockOwnerId;
    }

    public String getLockOwnerName() {
        return lockOwnerName;
    }

    public void setLockOwnerName(String lockOwnerName) {
        this.lockOwnerName = lockOwnerName;
    }

    public boolean isInNative() {
        return inNative;
    }

    public void setInNative(boolean inNative) {
        this.inNative = inNative;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    public MonitorInfo[] getLockedMonitors() {
        return lockedMonitors;
    }

    public void setLockedMonitors(MonitorInfo[] lockedMonitors) {
        this.lockedMonitors = lockedMonitors;
    }

    public LockInfo[] getLockedSynchronizers() {
        return lockedSynchronizers;
    }

    public void setLockedSynchronizers(LockInfo[] lockedSynchronizers) {
        this.lockedSynchronizers = lockedSynchronizers;
    }
}
