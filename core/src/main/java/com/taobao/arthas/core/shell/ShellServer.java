package com.taobao.arthas.core.shell;

import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.NoOpHandler;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;

/**
 * Shell服务器抽象类
 *
 * ShellServer是Arthas的核心服务器组件，负责管理多个终端服务器（TermServer）和Shell会话。
 * 它提供了创建Shell、管理命令解析器、控制作业生命周期等功能。
 *
 * <p>主要功能：</p>
 * <ul>
 * <li>管理多个TermServer（如Telnet、HTTP等不同协议的终端服务器）</li>
 * <li>为每个客户端连接创建独立的Shell实例和JobController</li>
 * <li>注册和管理命令解析器（CommandResolver）</li>
 * <li>控制服务器的启动和关闭</li>
 * <li>管理全局作业控制器和命令管理器</li>
 * </ul>
 *
 * <p>生命周期：</p>
 * <ol>
 * <li>通过create()方法创建ShellServer实例</li>
 * <li>注册TermServer和CommandResolver</li>
 * <li>调用listen()方法启动服务器</li>
 * <li>处理客户端连接，创建Shell会话</li>
 * <li>调用close()方法关闭服务器</li>
 * </ol>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class ShellServer {

    /**
     * 创建一个使用默认选项的Shell服务器
     *
     * 使用指定的配置选项创建ShellServer实例。配置选项包括会话超时时间、
     * 连接超时时间、欢迎消息等。实际返回的是ShellServerImpl的具体实现。
     *
     * @param options Shell服务器的配置选项，包含会话管理、超时设置等参数
     * @return 创建的Shell服务器实例
     */
    public static ShellServer create(ShellServerOptions options) {
        return new ShellServerImpl(options);
    }

    /**
     * 创建一个使用默认配置的Shell服务器
     *
     * 该方法使用默认的ShellServerOptions创建服务器实例，适用于不需要
     * 自定义配置的场景。默认配置包括合理的超时时间和标准欢迎消息。
     *
     * @return 使用默认配置创建的Shell服务器实例
     */
    public static ShellServer create() {
        return new ShellServerImpl(new ShellServerOptions());
    }

    /**
     * 注册命令解析器
     *
     * 向Shell服务器注册一个命令解析器，命令解析器负责将用户输入的命令
     * 解析为可执行的命令对象。可以注册多个命令解析器，它们会被依次调用
     * 直到找到能够处理该命令的解析器。
     *
     * @param resolver 命令解析器实例，用于解析和路由用户命令
     * @return 返回当前ShellServer实例，支持链式调用
     */
    public abstract ShellServer registerCommandResolver(CommandResolver resolver);

    /**
     * 注册终端服务器
     *
     * 向Shell服务器注册一个终端服务器（TermServer）。终端服务器负责
     * 接受客户端连接，可以支持不同的协议（如Telnet、HTTP等）。
     * 注册后，TermServer的生命周期由ShellServer管理，包括启动和关闭。
     *
     * @param termServer 要注册的终端服务器实例
     * @return 返回当前ShellServer实例，支持链式调用
     */
    public abstract ShellServer registerTermServer(TermServer termServer);

    /**
     * 创建一个新的Shell实例
     *
     * 为指定的终端（Term）创建一个独立的Shell会话。当有新的客户端连接时，
     * 会调用此方法创建对应的Shell实例。创建的Shell需要显式关闭以释放资源。
     *
     * @param term 与Shell关联的终端实例，代表客户端连接
     * @return 创建的Shell实例，该实例管理与该终端的所有交互
     */
    public abstract Shell createShell(Term term);

    /**
     * 创建一个新的Shell实例（无终端）
     *
     * 创建一个不关联具体终端的Shell实例，主要用于测试目的或特殊场景。
     * 返回的Shell应该显式关闭以释放相关资源。
     *
     * @return 创建的Shell实例
     */
    public abstract Shell createShell();

    /**
     * 启动Shell服务（异步，无回调）
     *
     * 启动Shell服务器，开始接受客户端连接。这是一个异步操作，服务器会
     * 在后台启动，不阻塞当前线程。使用无操作的回调处理器，不会收到启动完成通知。
     *
     * @return 返回当前ShellServer实例，支持链式调用
     */
    public ShellServer listen() {
        return listen(new NoOpHandler<Future<Void>>());
    }

    /**
     * 启动Shell服务（异步，带回调）
     *
     * 启动Shell服务器，开始接受客户端连接。这是一个异步操作，服务器会
     * 在后台启动。通过回调处理器可以获取启动完成的通知。
     *
     * @param listenHandler 启动完成后的回调处理器，用于接收启动结果
     * @return 返回当前ShellServer实例，支持链式调用
     */
    public abstract ShellServer listen(Handler<Future<Void>> listenHandler);

    /**
     * 关闭Shell服务器（异步，无回调）
     *
     * 关闭Shell服务器，停止接受新的连接，并关闭所有已注册的TermServer。
     * 这是一个异步操作，使用无操作的回调处理器，不会收到关闭完成通知。
     */
    public void close() {
        close(new NoOpHandler<Future<Void>>());
    }

    /**
     * 关闭Shell服务器（异步，带回调）
     *
     * 关闭Shell服务器，停止接受新的连接，并关闭所有已注册的TermServer。
     * 这是一个异步操作，服务器会在后台完成关闭流程。通过回调处理器可以
     * 获取关闭完成的通知。
     *
     * @param completionHandler 关闭完成后的回调处理器，用于接收关闭结果
     */
    public abstract void close(Handler<Future<Void>> completionHandler);

    /**
     * 获取全局作业控制器实例
     *
     * 返回ShellServer维护的全局作业控制器。该控制器管理所有Shell会话
     * 中的作业，提供跨会话的作业管理和监控功能。
     *
     * @return 全局作业控制器实例
     */
    public abstract JobControllerImpl getJobController();

    /**
     * 获取命令管理器实例
     *
     * 返回ShellServer的内部命令管理器，该管理器负责注册、查找和执行
     * 所有可用的命令。包括内置命令和用户自定义命令。
     *
     * @return 内部命令管理器实例
     */
    public abstract InternalCommandManager getCommandManager();
}
