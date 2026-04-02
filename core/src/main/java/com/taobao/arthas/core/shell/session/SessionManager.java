package com.taobao.arthas.core.shell.session;

import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

import java.lang.instrument.Instrumentation;

/**
 * Arthas 会话管理器接口
 *
 * 负责管理Arthas客户端与服务端之间的会话生命周期，包括会话的创建、获取、移除和关闭
 *
 * @author gongdewei 2020-03-20
 */
public interface SessionManager {

    /**
     * 创建一个新的会话
     *
     * @return 新创建的会话对象
     */
    Session createSession();

    /**
     * 根据会话ID获取指定会话
     *
     * @param sessionId 会话ID
     * @return 对应的会话对象，如果不存在则返回null
     */
    Session getSession(String sessionId);

    /**
     * 移除指定ID的会话
     * 移除操作会中断会话的前台任务并关闭相关的资源分发器
     *
     * @param sessionId 要移除的会话ID
     * @return 被移除的会话对象，如果会话不存在则返回null
     */
    Session removeSession(String sessionId);

    /**
     * 更新会话的最后访问时间
     * 用于会话超时检测，防止活跃会话被清理
     *
     * @param session 需要更新访问时间的会话对象
     */
    void updateAccessTime(Session session);

    /**
     * 关闭会话管理器并释放所有相关资源
     * 会关闭所有活跃会话，停止定时任务，并清理JobController
     */
    void close();

    /**
     * 获取内部命令管理器
     * 命令管理器负责注册和解析所有可用的Arthas命令
     *
     * @return 内部命令管理器实例
     */
    InternalCommandManager getCommandManager();

    /**
     * 获取Java Instrumentation实例
     * Instrumentation用于字节码增强和类重定义等操作
     *
     * @return Instrumentation实例
     */
    Instrumentation getInstrumentation();

    /**
     * 获取任务控制器
     * JobController负责管理所有后台和前台任务的执行
     *
     * @return 任务控制器实例
     */
    JobController getJobController();
}
