package com.taobao.arthas.core.command.model;

import java.lang.management.ThreadInfo;

/**
 * 线程阻塞锁信息
 *
 * 该类封装了线程阻塞时涉及的锁相关信息，从ThreadUtil中提取。
 * 主要用于诊断和展示线程因等待锁而阻塞的详细信息，包括持有锁的线程、
 * 锁的身份哈希码以及被阻塞的线程数量等。
 *
 * @author gongdewei 2020/7/14
 */
public class BlockingLockInfo {

    /**
     * 持有该锁的线程信息
     * 记录当前持有该锁的线程的详细信息，包括线程名称、状态、堆栈跟踪等
     */
    private ThreadInfo threadInfo = null;

    /**
     * 锁的身份哈希码
     * 锁对象的identityHashCode，用于唯一标识一个锁对象
     */
    private int lockIdentityHashCode = 0;

    /**
     * 被该锁阻塞的线程数量
     * 统计有多少个线程正在等待获取这个锁
     */
    private int blockingThreadCount = 0;

    /**
     * 默认构造函数
     * 创建一个空的BlockingLockInfo对象，所有字段使用默认值
     */
    public BlockingLockInfo() {
    }

    /**
     * 获取持有锁的线程信息
     *
     * @return 持有锁的线程信息对象
     */
    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

    /**
     * 设置持有锁的线程信息
     *
     * @param threadInfo 要设置的线程信息对象
     */
    public void setThreadInfo(ThreadInfo threadInfo) {
        this.threadInfo = threadInfo;
    }

    /**
     * 获取锁的身份哈希码
     *
     * @return 锁对象的identityHashCode
     */
    public int getLockIdentityHashCode() {
        return lockIdentityHashCode;
    }

    /**
     * 设置锁的身份哈希码
     *
     * @param lockIdentityHashCode 要设置的锁身份哈希码
     */
    public void setLockIdentityHashCode(int lockIdentityHashCode) {
        this.lockIdentityHashCode = lockIdentityHashCode;
    }

    /**
     * 获取被该锁阻塞的线程数量
     *
     * @return 被阻塞的线程数量
     */
    public int getBlockingThreadCount() {
        return blockingThreadCount;
    }

    /**
     * 设置被该锁阻塞的线程数量
     *
     * @param blockingThreadCount 要设置的被阻塞线程数量
     */
    public void setBlockingThreadCount(int blockingThreadCount) {
        this.blockingThreadCount = blockingThreadCount;
    }
}
