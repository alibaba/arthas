package com.taobao.arthas.core.command.model;

import java.lang.management.ThreadInfo;

/**
 * Thread blocking lock info, extract from ThreadUtil.
 *
 * @author gongdewei 2020/7/14
 */
public class BlockingLockInfo {

    // the thread info that is holing this lock.
    private ThreadInfo threadInfo = null;
    // the associated LockInfo object
    private int lockIdentityHashCode = 0;
    // the number of thread that is blocked on this lock
    private int blockingThreadCount = 0;

    public BlockingLockInfo() {
    }

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

    public void setThreadInfo(ThreadInfo threadInfo) {
        this.threadInfo = threadInfo;
    }

    public int getLockIdentityHashCode() {
        return lockIdentityHashCode;
    }

    public void setLockIdentityHashCode(int lockIdentityHashCode) {
        this.lockIdentityHashCode = lockIdentityHashCode;
    }

    public int getBlockingThreadCount() {
        return blockingThreadCount;
    }

    public void setBlockingThreadCount(int blockingThreadCount) {
        this.blockingThreadCount = blockingThreadCount;
    }
}
