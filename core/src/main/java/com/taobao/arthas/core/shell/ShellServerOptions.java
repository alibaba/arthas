package com.taobao.arthas.core.shell;

import com.taobao.arthas.core.util.ArthasBanner;

import java.lang.instrument.Instrumentation;

/**
 * Shell服务器配置选项类
 *
 * 该类封装了Shell服务器的所有配置参数，包括会话管理、超时设置、连接参数等。
 * 通过Builder模式的setter方法，可以方便地配置各种选项。
 *
 * <p>主要配置项：</p>
 * <ul>
 * <li>欢迎消息：用户连接时显示的欢迎信息</li>
 * <li>会话超时：空闲会话的过期时间</li>
 * <li>清理间隔：检查和清理过期会话的时间间隔</li>
 * <li>连接超时：等待客户端连接的超时时间</li>
 * <li>PID和Instrumentation：Java Agent相关配置</li>
 * </ul>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ShellServerOptions {

    /**
     * 默认的会话清理间隔（毫秒）
     * 定义了多久检查一次过期会话，默认为60秒
     */
    public static final long DEFAULT_REAPER_INTERVAL = 60 * 1000; // 60 seconds

    /**
     * 默认的会话超时时间（毫秒）
     * 定义了Shell会话在无访问情况下保持活跃的时长，默认为30分钟
     */
    public static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    /**
     * 默认的连接超时时间（毫秒）
     * 定义了服务器等待客户端连接的最大时长，默认为6秒
     */
    public static final long DEFAULT_CONNECTION_TIMEOUT = 6000; // 6 seconds

    /**
     * 默认的欢迎消息
     * 使用ArthasBanner工具类生成标准的欢迎信息
     */
    public static final String DEFAULT_WELCOME_MESSAGE = ArthasBanner.welcome();

    /**
     * 默认的Readline配置文件路径
     * 指定了Readline库的配置文件位置，用于定义命令行编辑行为
     */
    public static final String DEFAULT_INPUTRC = "com/taobao/arthas/core/shell/term/readline/inputrc";

    /**
     * Shell欢迎消息
     * 用户连接到Shell时显示的欢迎文本
     */
    private String welcomeMessage;

    /**
     * 会话超时时间（毫秒）
     * 定义了会话在无操作情况下自动过期的时间长度
     */
    private long sessionTimeout;

    /**
     * 会话清理间隔（毫秒）
     * 定义了定期检查和清理过期会话的时间间隔
     */
    private long reaperInterval;

    /**
     * 连接超时时间（毫秒）
     * 定义了服务器等待客户端建立连接的最大时长
     */
    private long connectionTimeout;

    /**
     * 进程ID
     * 当前Java进程的标识符，用于标识和管理目标JVM进程
     */
    private long pid;

    /**
     * Java Instrumentation实例
     * 提供Java Agent的Instrumentation功能，用于字节码增强和类重定义
     */
    private Instrumentation instrumentation;

    /**
     * 构造函数
     *
     * 创建一个使用默认配置的ShellServerOptions实例。
     * 所有配置项都会被初始化为预定义的默认值。
     */
    public ShellServerOptions() {
        welcomeMessage = DEFAULT_WELCOME_MESSAGE;
        sessionTimeout = DEFAULT_SESSION_TIMEOUT;
        connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        reaperInterval = DEFAULT_REAPER_INTERVAL;
    }

    /**
     * 获取Shell欢迎消息
     *
     * @return 当前的欢迎消息文本，用户连接时显示
     */
    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    /**
     * 设置Shell欢迎消息
     *
     * 设置用户连接到Shell时显示的欢迎消息。该消息可以包含
     * 使用说明、版本信息、帮助提示等内容。
     *
     * @param welcomeMessage 要设置的欢迎消息文本
     * @return 返回当前ShellServerOptions实例，支持链式调用
     */
    public ShellServerOptions setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
        return this;
    }

    /**
     * 获取会话超时时间
     *
     * @return 会话超时时间（毫秒），超时后空闲会话将被自动关闭
     */
    public long getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * 设置会话超时时间
     *
     * 设置Shell会话的超时时长。如果会话在指定时间内没有任何活动，
     * 将被自动关闭以释放资源。
     *
     * @param sessionTimeout 新的会话超时时间（毫秒）
     * @return 返回当前ShellServerOptions实例，支持链式调用
     */
    public ShellServerOptions setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    /**
     * 获取会话清理间隔
     *
     * @return 会话清理间隔（毫秒），定义了检查过期会话的频率
     */
    public long getReaperInterval() {
        return reaperInterval;
    }

    /**
     * 设置会话清理间隔
     *
     * 设置执行会话清理任务的时间间隔。清理任务会检查所有会话，
     * 关闭那些超过超时时间的空闲会话。
     *
     * @param reaperInterval 新的清理间隔时间（毫秒）
     * @return 返回当前ShellServerOptions实例，支持链式调用
     */
    public ShellServerOptions setReaperInterval(long reaperInterval) {
        this.reaperInterval = reaperInterval;
        return this;
    }

    /**
     * 设置进程ID
     *
     * 设置当前Java进程的PID。该ID用于标识目标JVM进程，
     * 在某些操作中需要使用进程ID进行系统级操作。
     *
     * @param pid 进程ID
     * @return 返回当前ShellServerOptions实例，支持链式调用
     */
    public ShellServerOptions setPid(long pid) {
        this.pid = pid;
        return this;
    }

    /**
     * 设置Java Instrumentation实例
     *
     * 设置Java Agent的Instrumentation实例。该实例提供了字节码
     * 增强和类重定义的能力，是Arthas实现类增强和监控的基础。
     *
     * @param instrumentation Java Instrumentation实例
     * @return 返回当前ShellServerOptions实例，支持链式调用
     */
    public ShellServerOptions setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        return this;
    }

    /**
     * 获取进程ID
     *
     * @return 当前配置的进程ID，用于标识目标JVM进程
     */
    public long getPid() {
        return pid;
    }

    /**
     * 获取Java Instrumentation实例
     *
     * @return Java Instrumentation实例，用于字节码操作和类重定义
     */
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * 获取连接超时时间
     *
     * @return 连接超时时间（毫秒），服务器等待客户端连接的最大时长
     */
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * 设置连接超时时间
     *
     * 设置服务器等待客户端建立连接的最大时长。如果在指定时间内
     * 客户端未能完成连接，连接请求将被超时处理。
     *
     * @param connectionTimeout 新的连接超时时间（毫秒）
     */
    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
