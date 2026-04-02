package com.taobao.arthas.core.shell.system;

import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.term.Term;

import java.util.List;
import java.util.Set;

/**
 * 任务控制器接口
 * <p>
 * 定义了任务（Job）的创建、管理和生命周期控制的核心接口。
 * 任务控制器负责管理Arthas中的所有后台任务，包括前台任务和后台任务。
 * </p>
 * <p>
 * 主要职责：
 * <ul>
 * <li>创建和管理任务实例</li>
 * <li>跟踪活动的任务状态</li>
 * <li>控制任务的启动、停止和终止</li>
 * <li>管理任务的生命周期事件</li>
 * </ul>
 * </p>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface JobController {

    /**
     * 获取所有活动的任务
     * <p>
     * 返回当前会话中所有正在运行的活动任务集合。
     * 活动任务包括正在执行的前台任务和后台任务。
     * </p>
     *
     * @return 所有活动任务的集合
     */
    Set<Job> jobs();

    /**
     * 根据ID获取活动任务
     * <p>
     * 返回当前会话中指定ID的活动任务。
     * 如果找不到对应ID的任务，则返回null。
     * </p>
     *
     * @param id 任务ID
     * @return 找到的任务对象，如果未找到则返回null
     */
    Job getJob(int id);

    /**
     * 创建新任务
     * <p>
     * 创建一个包装进程的新任务。该方法会初始化任务的所有必要组件，
     * 包括命令管理器、命令令牌、会话、事件监听器、终端和结果分发器。
     * </p>
     *
     * @param commandManager 内部命令管理器，用于管理和执行命令
     * @param tokens 命令令牌列表，包含命令及其参数的解析结果
     * @param session 当前Shell会话对象，包含会话的状态和配置信息
     * @param jobHandler 任务事件处理器，用于处理任务的生命周期事件
     * @param term 终端对象，用于与用户交互（Telnet终端）
     * @param resultDistributor 结果分发器，用于分发命令执行的结果
     * @return 创建的新任务对象
     */
    Job createJob(InternalCommandManager commandManager, List<CliToken> tokens, Session session, JobListener jobHandler, Term term, ResultDistributor resultDistributor);

    /**
     * 关闭任务控制器
     * <p>
     * 关闭控制器并终止所有底层任务。
     * 关闭后的控制器将不再接受新的任务创建请求。
     * 所有正在运行的任务都会被终止。
     * </p>
     * <p>
     * 该方法是异步操作，关闭完成后会调用completionHandler回调。
     * </p>
     *
     * @param completionHandler 完成处理器，当控制器关闭完成后被调用
     */
    void close(Handler<Void> completionHandler);

    /**
     * 关闭Shell会话
     * <p>
     * 关闭Shell会话并终止所有底层任务。
     * 这是一个同步操作，会立即终止所有正在运行的任务。
     * </p>
     * <p>
     * 与带参数的close方法不同，此方法不需要完成处理器，
     * 会立即执行关闭操作。
     * </p>
     */
    void close();

}
