package com.taobao.arthas.core.shell;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;

import java.util.List;

/**
 * Shell接口
 *
 * 该接口定义了用户与Shell之间交互会话的抽象。
 * Shell是Arthas核心交互机制，负责管理命令执行、会话状态和作业控制。
 * 每个Shell实例对应一个独立的用户会话，提供命令执行、作业管理等功能。
 *
 * 主要职责：
 * - 创建和管理作业（Job）
 * - 控制作业生命周期
 * - 管理会话状态
 * - 处理Shell关闭逻辑
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Shell {

    /**
     * 创建一个作业
     *
     * 根据命令行Token列表创建一个新的作业实例。创建的作业需要通过
     * {@link Job#run()}方法显式执行。此方法支持复杂的命令行解析，
     * 可以处理命令参数、选项等Token级别的详细信息。
     *
     * @param line 命令行的Token列表，包含命令名称、参数、选项等信息
     * @return 创建的作业对象，该对象尚未执行，需要调用run()方法启动
     */
    Job createJob(List<CliToken> line);

    /**
     * 创建一个作业（简化版本）
     *
     * 该方法是{@link #createJob(List)}的便捷版本，直接接受字符串形式的命令行。
     * 内部会将字符串解析为Token列表，然后调用createJob(List<CliToken>)方法。
     *
     * @param line 命令行字符串，格式为用户直接输入的命令文本
     * @return 创建的作业对象，该对象尚未执行，需要调用run()方法启动
     * @see #createJob(List)
     */
    Job createJob(String line);

    /**
     * 获取Shell的作业控制器
     *
     * 返回与此Shell关联的作业控制器实例。作业控制器负责管理所有作业的生命周期，
     * 包括作业的创建、执行、取消、监控等操作。通过作业控制器可以实现对多个作业
     * 的统一管理和调度。
     *
     * @return 当前Shell的作业控制器实例
     */
    JobController jobController();

    /**
     * 获取当前Shell会话
     *
     * 返回当前Shell的会话对象。会话对象保存了Shell的状态信息，包括：
     * - 用户上下文信息
     * - 环境变量
     * - 历史命令记录
     * - 会话级别的配置
     *
     * @return 当前Shell的会话对象
     */
    Session session();

    /**
     * 关闭Shell
     *
     * 关闭当前Shell会话，释放相关资源。此方法会：
     * - 停止所有正在运行的作业
     * - 清理会话资源
     * - 断开客户端连接
     * - 记录关闭原因
     *
     * @param reason 关闭Shell的原因描述，用于日志记录和调试
     */
    void close(String reason);
}

