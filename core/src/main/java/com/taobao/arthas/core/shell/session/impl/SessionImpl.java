package com.taobao.arthas.core.shell.session.impl;

import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Session接口的实现类
 *
 * 提供了会话管理的基本功能，包括数据存储、锁机制、
 * 生命周期管理等
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SessionImpl implements Session {
    /** 锁序列号生成器，用于生成唯一的锁序列ID */
    private final static AtomicInteger lockSequence = new AtomicInteger();

    /** 空锁的标识值，表示会话未被锁定 */
    private final static int LOCK_TX_EMPTY = -1;

    /** 锁状态，使用原子整数保证线程安全 */
    private final AtomicInteger lock = new AtomicInteger(LOCK_TX_EMPTY);

    /** 会话数据存储，使用并发HashMap保证线程安全 */
    private Map<String, Object> data = new ConcurrentHashMap<String, Object>();

    /**
     * 构造函数，创建一个新的会话
     * 初始化创建时间和最后访问时间
     */
    public SessionImpl() {
        long now = System.currentTimeMillis();
        data.put(CREATE_TIME, now);
        this.setLastAccessTime(now);
    }

    /**
     * 向会话中存放数据
     * 如果值为null，则移除该键
     *
     * @param key 数据的键
     * @param obj 要存储的数据对象
     * @return 当前会话对象
     */
    @Override
    public Session put(String key, Object obj) {
        if (obj == null) {
            data.remove(key);
        } else {
            data.put(key, obj);
        }
        return this;
    }

    /**
     * 从会话中获取数据
     *
     * @param key 数据的键
     * @return 数据对象，如果不存在则返回null
     * @param <T> 返回的数据类型
     */
    @Override
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /**
     * 从会话中移除数据
     *
     * @param key 数据的键
     * @return 被移除的数据，如果不存在则返回null
     * @param <T> 返回的数据类型
     */
    @Override
    public <T> T remove(String key) {
        return (T) data.remove(key);
    }

    /**
     * 尝试获取会话的锁
     * 使用CAS操作，只有锁状态为LOCK_TX_EMPTY时才能成功获取
     *
     * @return true表示成功获取锁，false表示锁已被占用
     */
    @Override
    public boolean tryLock() {
        return lock.compareAndSet(LOCK_TX_EMPTY, lockSequence.getAndIncrement());
    }

    /**
     * 解锁会话
     * 使用CAS操作，只有当前锁序列匹配时才能成功解锁
     *
     * @throws IllegalStateException 如果锁已被其他线程持有或会话未锁定
     */
    @Override
    public void unLock() {
        int currentLockTx = lock.get();
        if (!lock.compareAndSet(currentLockTx, LOCK_TX_EMPTY)) {
            throw new IllegalStateException();
        }
    }

    /**
     * 检查会话是否已被锁定
     *
     * @return true表示已锁定，false表示未锁定
     */
    @Override
    public boolean isLocked() {
        return lock.get() != LOCK_TX_EMPTY;
    }

    /**
     * 获取当前锁的序列ID
     *
     * @return 锁的序列ID，-1表示未锁定
     */
    @Override
    public int getLock() {
        return lock.get();
    }

    /**
     * 获取会话ID
     *
     * @return 会话ID字符串
     */
    @Override
    public String getSessionId() {
        return (String) data.get(ID);
    }

    /**
     * 获取Java进程ID
     *
     * @return Java进程PID
     */
    @Override
    public long getPid() {
        return (Long) data.get(PID);
    }

    /**
     * 获取所有已注册的命令解析器
     *
     * @return 命令解析器列表
     */
    @Override
    public List<CommandResolver> getCommandResolvers() {
        InternalCommandManager commandManager = (InternalCommandManager) data.get(COMMAND_MANAGER);
        return commandManager.getResolvers();
    }

    /**
     * 获取Java Instrumentation实例
     *
     * @return Instrumentation实例
     */
    @Override
    public Instrumentation getInstrumentation() {
        return (Instrumentation) data.get(INSTRUMENTATION);
    }

    /**
     * 更新会话的最后访问时间
     *
     * @param time 新的访问时间（毫秒时间戳）
     */
    @Override
    public void setLastAccessTime(long time) {
        this.put(LAST_ACCESS_TIME, time);
    }

    /**
     * 获取会话的最后访问时间
     *
     * @return 最后访问时间（毫秒时间戳）
     */
    @Override
    public long getLastAccessTime() {
        return (Long)data.get(LAST_ACCESS_TIME);
    }

    /**
     * 获取会话的创建时间
     *
     * @return 创建时间（毫秒时间戳）
     */
    @Override
    public long getCreateTime() {
        return (Long)data.get(CREATE_TIME);
    }

    /**
     * 更新会话的命令结果分发器
     *
     * @param resultDistributor 结果分发器，如果为null则移除
     */
    @Override
    public void setResultDistributor(SharingResultDistributor resultDistributor) {
        if (resultDistributor == null) {
            data.remove(RESULT_DISTRIBUTOR);
        } else {
            data.put(RESULT_DISTRIBUTOR, resultDistributor);
        }
    }

    /**
     * 获取会话的命令结果分发器
     *
     * @return 结果分发器
     */
    @Override
    public SharingResultDistributor getResultDistributor() {
        return (SharingResultDistributor) data.get(RESULT_DISTRIBUTOR);
    }

    /**
     * 设置前台作业
     *
     * @param job 要设置为前台的作业，如果为null则移除
     */
    @Override
    public void setForegroundJob(Job job) {
        if (job == null) {
            data.remove(FOREGROUND_JOB);
        } else {
            data.put(FOREGROUND_JOB, job);
        }
    }

    /**
     * 获取当前的前台作业
     *
     * @return 前台作业，如果没有则返回null
     */
    @Override
    public Job getForegroundJob() {
        return (Job) data.get(FOREGROUND_JOB);
    }

    /**
     * 判断会话是否是TTY终端
     *
     * @return true表示是TTY终端
     */
    @Override
    public boolean isTty() {
        return get(TTY) != null;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID字符串
     */
    @Override
    public String getUserId() {
        return (String) data.get(USER_ID);
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户ID，如果为null则移除
     */
    @Override
    public void setUserId(String userId) {
        if (userId == null) {
            data.remove(USER_ID);
        } else {
            data.put(USER_ID, userId);
        }
    }

}
