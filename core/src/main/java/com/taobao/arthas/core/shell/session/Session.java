package com.taobao.arthas.core.shell.session;

import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.system.Job;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * Shell会话接口
 *
 * Session代表一个Arthas的Shell会话，用于存储会话相关的数据和状态。
 * 它提供了键值对存储、锁机制、会话生命周期管理等功能。
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author gongdewei 2020-03-23
 */
public interface Session {
    /** 命令管理器的键名 */
    String COMMAND_MANAGER = "arthas-command-manager";

    /** Java进程ID的键名 */
    String PID = "pid";

    /** Java Instrumentation实例的键名 */
    String INSTRUMENTATION = "instrumentation";

    /** 会话ID的键名 */
    String ID = "id";

    /** 服务器对象的键名 */
    String SERVER = "server";

    /** 用户ID的键名 */
    String USER_ID = "userId";

    /** 终端设备的键名 */
    String TTY = "tty";

    /** 会话创建时间的键名 */
    String CREATE_TIME = "createTime";

    /** 会话最后访问时间的键名 */
    String LAST_ACCESS_TIME = "lastAccessedTime";

    /** 结果分发器的键名 */
    String RESULT_DISTRIBUTOR = "resultDistributor";

    /** 前台作业的键名 */
    String FOREGROUND_JOB = "foregroundJob";


    /**
     * 向会话中存放数据
     *
     * @param key 数据的键
     * @param obj 要存储的数据对象
     * @return 当前会话对象，支持链式调用
     */
    Session put(String key, Object obj);

    /**
     * 从会话中获取数据
     *
     * @param key 数据的键
     * @return 数据对象，如果不存在则返回null
     * @param <T> 返回的数据类型
     */
    <T> T get(String key);

    /**
     * 从会话中移除数据
     *
     * @param key 数据的键
     * @return 被移除的数据，如果不存在则返回null
     * @param <T> 返回的数据类型
     */
    <T> T remove(String key);

    /**
     * 检查会话是否已被锁定
     *
     * @return true表示已锁定，false表示未锁定
     */
    boolean isLocked();

    /**
     * 解锁会话
     * 如果会话未被锁定或锁定序列不匹配，将抛出异常
     */
    void unLock();

    /**
     * 尝试获取会话的锁
     * 使用CAS操作，只有一个调用者能成功获取锁
     *
     * @return true表示成功获取锁，false表示锁已被占用
     */
    boolean tryLock();

    /**
     * 获取当前锁的序列ID
     *
     * @return 锁的序列ID，-1表示未锁定
     */
    int getLock();

    /**
     * 获取会话ID
     *
     * @return 会话ID字符串
     */
    String getSessionId();

    /**
     * 获取Java进程ID
     *
     * @return Java进程PID
     */
    long getPid();

    /**
     * 获取所有已注册的命令解析器
     *
     * @return 命令解析器列表
     */
    List<CommandResolver> getCommandResolvers();

    /**
     * 获取Java Instrumentation实例
     * Instrumentation用于字节码增强和类重定义
     *
     * @return Instrumentation实例
     */
    Instrumentation getInstrumentation();

    /**
     * 更新会话的最后访问时间
     *
     * @param time 新的访问时间（毫秒时间戳）
     */
    void setLastAccessTime(long time);

    /**
     * 获取会话的最后访问时间
     *
     * @return 最后访问时间（毫秒时间戳）
     */
    long getLastAccessTime();

    /**
     * 获取会话的创建时间
     *
     * @return 创建时间（毫秒时间戳）
     */
    long getCreateTime();

    /**
     * 更新会话的命令结果分发器
     *
     * @param resultDistributor 结果分发器
     */
    void setResultDistributor(SharingResultDistributor resultDistributor);

    /**
     * 获取会话的命令结果分发器
     *
     * @return 结果分发器
     */
    SharingResultDistributor getResultDistributor();

    /**
     * 设置前台作业
     *
     * @param job 要设置为前台的作业
     */
    void setForegroundJob(Job job);

    /**
     * 获取当前的前台作业
     *
     * @return 前台作业，如果没有则返回null
     */
    Job getForegroundJob();

    /**
     * 判断会话是否是TTY终端
     *
     * @return true表示是TTY终端
     */
    boolean isTty();

    /**
     * 获取用户ID
     *
     * @return 用户ID字符串
     */
    String getUserId();

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    void setUserId(String userId);
}
