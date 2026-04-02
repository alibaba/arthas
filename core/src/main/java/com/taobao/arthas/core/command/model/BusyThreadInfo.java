package com.taobao.arthas.core.command.model;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/**
 * 忙碌线程信息模型类
 * <p>
 * 该类用于封装忙碌线程的详细信息，继承自ThreadVO类，
 * 包含了ThreadInfo中的所有字段以及线程的阻塞和等待信息。
 * 主要用于在Arthas诊断工具中展示线程的运行状态和锁相关信息。
 * </p>
 *
 * @author gongdewei 2020/4/26
 */
public class BusyThreadInfo extends ThreadVO {

    /**
     * 线程阻塞的总时间（毫秒）
     * 表示线程因为进入同步块/方法而被阻塞的总时长
     */
    private long         blockedTime;

    /**
     * 线程阻塞的总次数
     * 表示线程因为进入同步块/方法而被阻塞的总次数
     */
    private long         blockedCount;

    /**
     * 线程等待的总时间（毫秒）
     * 表示线程因为调用Object.wait()或Thread.join()等方法而等待的总时长
     */
    private long         waitedTime;

    /**
     * 线程等待的总次数
     * 表示线程因为调用Object.wait()或Thread.join()等方法而等待的总次数
     */
    private long         waitedCount;

    /**
     * 锁信息对象
     * 表示线程正在等待或被阻塞的锁的详细信息
     */
    private LockInfo lockInfo;

    /**
     * 锁的名称
     * 表示线程正在等待的锁的字符串表示形式
     */
    private String       lockName;

    /**
     * 锁持有者的线程ID
     * 表示当前持有该锁的线程的唯一标识符
     */
    private long         lockOwnerId;

    /**
     * 锁持有者的线程名称
     * 表示当前持有该锁的线程的名称
     */
    private String       lockOwnerName;

    /**
     * 是否在本地代码中执行
     * 表示线程当前是否在执行本地（Native）代码
     */
    private boolean      inNative;

    /**
     * 线程是否被挂起
     * 表示线程是否已被挂起
     */
    private boolean      suspended;

    /**
     * 线程的堆栈跟踪信息
     * 包含线程调用堆栈中每个方法的StackTraceElement对象数组
     */
    private StackTraceElement[] stackTrace;

    /**
     * 线程锁定的监视器数组
     * 包含线程当前持有的所有监视器对象的信息
     */
    private MonitorInfo[]       lockedMonitors;

    /**
     * 线程锁定的同步器数组
     * 包含线程当前持有的所有同步器（如ReentrantLock）的信息
     */
    private LockInfo[]          lockedSynchronizers;


    /**
     * 构造函数：创建忙碌线程信息对象
     * <p>
     * 根据ThreadVO对象和ThreadInfo对象构建完整的忙碌线程信息。
     * 将ThreadVO中的基础线程信息和ThreadInfo中的详细锁信息合并到当前对象中。
     * </p>
     *
     * @param thread     ThreadVO对象，包含线程的基础信息（如ID、名称、状态、CPU使用等）
     * @param threadInfo ThreadInfo对象，包含线程的详细锁信息和堆栈跟踪信息
     */
    public BusyThreadInfo(ThreadVO thread, ThreadInfo threadInfo) {
        // 从ThreadVO对象复制基础线程信息
        this.setId(thread.getId());                    // 设置线程ID
        this.setName(thread.getName());                // 设置线程名称
        this.setDaemon(thread.isDaemon());            // 设置是否为守护线程
        this.setInterrupted(thread.isInterrupted());  // 设置是否被中断
        this.setPriority(thread.getPriority());        // 设置线程优先级
        this.setGroup(thread.getGroup());             // 设置线程组
        this.setState(thread.getState());             // 设置线程状态
        this.setCpu(thread.getCpu());                 // 设置CPU使用情况
        this.setDeltaTime(thread.getDeltaTime());      // 设置时间增量
        this.setTime(thread.getTime());                // 设置线程运行时间

        // 如果ThreadInfo对象不为空，则复制锁和堆栈相关信息
        if (threadInfo != null) {
            this.setLockInfo(threadInfo.getLockInfo());                        // 设置锁信息
            this.setLockedMonitors(threadInfo.getLockedMonitors());            // 设置锁定的监视器
            this.setLockedSynchronizers(threadInfo.getLockedSynchronizers());  // 设置锁定的同步器
            this.setLockName(threadInfo.getLockName());                        // 设置锁名称
            this.setLockOwnerId(threadInfo.getLockOwnerId());                  // 设置锁持有者ID
            this.setLockOwnerName(threadInfo.getLockOwnerName());              // 设置锁持有者名称
            this.setStackTrace(threadInfo.getStackTrace());                    // 设置堆栈跟踪信息
            this.setBlockedCount(threadInfo.getBlockedCount());                // 设置阻塞次数
            this.setBlockedTime(threadInfo.getBlockedTime());                  // 设置阻塞时间
            this.setInNative(threadInfo.isInNative());                          // 设置是否在本地代码中执行
            this.setSuspended(threadInfo.isSuspended());                        // 设置是否被挂起
            this.setWaitedCount(threadInfo.getWaitedCount());                  // 设置等待次数
            this.setWaitedTime(threadInfo.getWaitedTime());                    // 设置等待时间
        }

    }

    /**
     * 获取线程阻塞的总时间
     *
     * @return 线程阻塞的总时间（毫秒）
     */
    public long getBlockedTime() {
        return blockedTime;
    }

    /**
     * 设置线程阻塞的总时间
     *
     * @param blockedTime 线程阻塞的总时间（毫秒）
     */
    public void setBlockedTime(long blockedTime) {
        this.blockedTime = blockedTime;
    }

    /**
     * 获取线程阻塞的总次数
     *
     * @return 线程阻塞的总次数
     */
    public long getBlockedCount() {
        return blockedCount;
    }

    /**
     * 设置线程阻塞的总次数
     *
     * @param blockedCount 线程阻塞的总次数
     */
    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }

    /**
     * 获取线程等待的总时间
     *
     * @return 线程等待的总时间（毫秒）
     */
    public long getWaitedTime() {
        return waitedTime;
    }

    /**
     * 设置线程等待的总时间
     *
     * @param waitedTime 线程等待的总时间（毫秒）
     */
    public void setWaitedTime(long waitedTime) {
        this.waitedTime = waitedTime;
    }

    /**
     * 获取线程等待的总次数
     *
     * @return 线程等待的总次数
     */
    public long getWaitedCount() {
        return waitedCount;
    }

    /**
     * 设置线程等待的总次数
     *
     * @param waitedCount 线程等待的总次数
     */
    public void setWaitedCount(long waitedCount) {
        this.waitedCount = waitedCount;
    }

    /**
     * 获取锁信息对象
     *
     * @return 锁信息对象
     */
    public LockInfo getLockInfo() {
        return lockInfo;
    }

    /**
     * 设置锁信息对象
     *
     * @param lockInfo 锁信息对象
     */
    public void setLockInfo(LockInfo lockInfo) {
        this.lockInfo = lockInfo;
    }

    /**
     * 获取锁的名称
     *
     * @return 锁的名称字符串
     */
    public String getLockName() {
        return lockName;
    }

    /**
     * 设置锁的名称
     *
     * @param lockName 锁的名称字符串
     */
    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    /**
     * 获取锁持有者的线程ID
     *
     * @return 锁持有者的线程ID
     */
    public long getLockOwnerId() {
        return lockOwnerId;
    }

    /**
     * 设置锁持有者的线程ID
     *
     * @param lockOwnerId 锁持有者的线程ID
     */
    public void setLockOwnerId(long lockOwnerId) {
        this.lockOwnerId = lockOwnerId;
    }

    /**
     * 获取锁持有者的线程名称
     *
     * @return 锁持有者的线程名称
     */
    public String getLockOwnerName() {
        return lockOwnerName;
    }

    /**
     * 设置锁持有者的线程名称
     *
     * @param lockOwnerName 锁持有者的线程名称
     */
    public void setLockOwnerName(String lockOwnerName) {
        this.lockOwnerName = lockOwnerName;
    }

    /**
     * 判断线程是否在本地代码中执行
     *
     * @return 如果线程在本地代码中执行则返回true，否则返回false
     */
    public boolean isInNative() {
        return inNative;
    }

    /**
     * 设置线程是否在本地代码中执行
     *
     * @param inNative 是否在本地代码中执行
     */
    public void setInNative(boolean inNative) {
        this.inNative = inNative;
    }

    /**
     * 判断线程是否被挂起
     *
     * @return 如果线程被挂起则返回true，否则返回false
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * 设置线程是否被挂起
     *
     * @param suspended 是否被挂起
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * 获取线程的堆栈跟踪信息
     *
     * @return 堆栈跟踪元素数组
     */
    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    /**
     * 设置线程的堆栈跟踪信息
     *
     * @param stackTrace 堆栈跟踪元素数组
     */
    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    /**
     * 获取线程锁定的监视器数组
     *
     * @return 锁定的监视器数组
     */
    public MonitorInfo[] getLockedMonitors() {
        return lockedMonitors;
    }

    /**
     * 设置线程锁定的监视器数组
     *
     * @param lockedMonitors 锁定的监视器数组
     */
    public void setLockedMonitors(MonitorInfo[] lockedMonitors) {
        this.lockedMonitors = lockedMonitors;
    }

    /**
     * 获取线程锁定的同步器数组
     *
     * @return 锁定的同步器数组
     */
    public LockInfo[] getLockedSynchronizers() {
        return lockedSynchronizers;
    }

    /**
     * 设置线程锁定的同步器数组
     *
     * @param lockedSynchronizers 锁定的同步器数组
     */
    public void setLockedSynchronizers(LockInfo[] lockedSynchronizers) {
        this.lockedSynchronizers = lockedSynchronizers;
    }
}
