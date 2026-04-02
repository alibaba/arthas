package com.taobao.arthas.core.shell.term;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.impl.TelnetTermServer;

/**
 * 终端服务器抽象类
 *
 * 用于基于终端的应用程序的服务器基类。提供了创建不同协议终端服务器的静态方法，
 * 以及处理终端连接、监听端口、关闭服务器等抽象方法。
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TermServer {

    /**
     * 创建 Telnet 协议的终端服务器
     *
     * 根据配置信息创建一个 Telnet 终端服务器实例。如果配置中没有指定端口号，
     * 则使用默认的 TELNET_PORT。
     *
     * @param configure Arthas 配置对象，包含 IP 地址和端口配置
     * @param options Shell 服务器选项，包含连接超时等配置
     * @return 创建的 Telnet 终端服务器实例
     */
    public static TermServer createTelnetTermServer(Configure configure, ShellServerOptions options) {
        // 如果配置中指定了 Telnet 端口则使用指定的端口，否则使用默认端口
        int port = configure.getTelnetPort() != null ? configure.getTelnetPort() : ArthasConstants.TELNET_PORT;
        // 创建并返回 Telnet 终端服务器实例
        return new TelnetTermServer(configure.getIp(), port, options.getConnectionTimeout());
    }

    /**
     * 创建 HTTP 协议的终端服务器
     *
     * 使用现有的路由器创建 HTTP 终端服务器（此功能尚未实现）。
     *
     * @return HTTP 终端服务器实例（当前返回 null，功能待实现）
     */
    public static TermServer createHttpTermServer() {
        // TODO: 待实现 HTTP 终端服务器
        return null;
    }

    /**
     * 设置终端处理器，用于接收传入的客户端连接
     *
     * 当远程终端连接时，会调用此处理器，并传入 {@link Term} 对象，
     * 该对象可用于与远程终端进行交互。
     *
     * @param handler 终端处理器，用于处理连接的终端
     * @return 当前对象，支持链式调用
     */
    public abstract TermServer termHandler(Handler<Term> handler);

    /**
     * 绑定并启动终端服务器
     *
     * 启动服务器监听，在此之前必须先通过 {@link #termHandler(Handler)} 设置终端处理器。
     * 此方法不提供完成回调，适用于不需要监听完成事件的场景。
     *
     * @return 当前对象，支持链式调用
     */
    public TermServer listen() {
        // 调用带监听处理器的方法，传入 null 表示不需要回调
        return listen(null);
    }

    /**
     * 绑定并启动终端服务器（带完成回调）
     *
     * 启动服务器监听，在此之前必须先通过 {@link #termHandler(Handler)} 设置终端处理器。
     * 可以传入一个监听处理器，当服务器绑定完成时会触发该处理器。
     *
     * @param listenHandler 监听完成处理器，用于处理服务器绑定完成事件
     * @return 当前对象，支持链式调用
     */
    public abstract TermServer listen(Handler<Future<TermServer>> listenHandler);

    /**
     * 获取服务器实际监听的端口号
     *
     * 返回服务器实际监听的端口号。如果在绑定时指定端口为 0（表示使用临时端口），
     * 此方法可以获取系统分配的实际端口号。
     *
     * @return 服务器实际监听的端口号
     */
    public abstract int actualPort();

    /**
     * 关闭终端服务器
     *
     * 关闭服务器，这将关闭所有当前打开的连接。
     * 注意：关闭操作可能在此方法返回后才完成。
     */
    public abstract void close();

    /**
     * 关闭终端服务器（带完成回调）
     *
     * 与 {@link #close()} 类似，但提供一个处理器，在服务器完全关闭时会被通知。
     *
     * @param completionHandler 完成处理器，当终端服务器关闭完成时会被调用
     */
    public abstract void close(Handler<Future<Void>> completionHandler);

}
