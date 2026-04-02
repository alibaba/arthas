package com.taobao.arthas.core.shell.term.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import io.termd.core.function.Consumer;
import io.termd.core.telnet.netty.NettyTelnetTtyBootstrap;
import io.termd.core.tty.TtyConnection;

import java.util.concurrent.TimeUnit;

/**
 * Telnet终端服务器实现类
 *
 * 封装了Telnet服务器的设置和启动逻辑，用于监听Telnet连接并创建终端会话
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetTermServer extends TermServer {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(TelnetTermServer.class);

    /**
     * Netty Telnet引导类，用于启动和管理Telnet服务器
     */
    private NettyTelnetTtyBootstrap bootstrap;

    /**
     * 监听的主机IP地址
     */
    private String hostIp;

    /**
     * 监听的端口号
     */
    private int port;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectionTimeout;

    /**
     * 终端处理器，当有新的Telnet连接建立时被调用
     */
    private Handler<Term> termHandler;

    /**
     * 构造函数
     *
     * @param hostIp 监听的主机IP地址
     * @param port 监听的端口号
     * @param connectionTimeout 连接超时时间（毫秒）
     */
    public TelnetTermServer(String hostIp, int port, long connectionTimeout) {
        this.hostIp = hostIp;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * 设置终端处理器
     *
     * @param handler 终端处理器，用于处理新创建的终端
     * @return 返回当前实例，支持链式调用
     */
    @Override
    public TermServer termHandler(Handler<Term> handler) {
        termHandler = handler;
        return this;
    }

    /**
     * 启动Telnet服务器并开始监听连接
     *
     * @param listenHandler 监听完成后的回调处理器
     * @return 返回当前实例，支持链式调用
     */
    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        // TODO: 从配置选项中获取字符集和inputrc配置
        // 创建并配置Netty Telnet引导类
        bootstrap = new NettyTelnetTtyBootstrap().setHost(hostIp).setPort(port);
        try {
            // 启动服务器，设置连接处理器
            bootstrap.start(new Consumer<TtyConnection>() {
                @Override
                public void accept(final TtyConnection conn) {
                    // 当有新连接时，创建TermImpl实例并调用处理器
                    termHandler.handle(new TermImpl(Helper.loadKeymap(), conn));
                }
            }).get(connectionTimeout, TimeUnit.MILLISECONDS);
            // 监听成功，返回成功Future
            listenHandler.handle(Future.<TermServer>succeededFuture());
        } catch (Throwable t) {
            // 监听失败，记录错误并返回失败Future
            logger.error("Error listening to port " + port, t);
            listenHandler.handle(Future.<TermServer>failedFuture(t));
        }
        return this;
    }

    /**
     * 关闭Telnet服务器
     */
    @Override
    public void close() {
        close(null);
    }

    /**
     * 关闭Telnet服务器并完成回调
     *
     * @param completionHandler 关闭完成后的回调处理器
     */
    @Override
    public void close(Handler<Future<Void>> completionHandler) {
        if (bootstrap != null) {
            // 停止服务器
            bootstrap.stop();
            if (completionHandler != null) {
                // 返回成功结果
                completionHandler.handle(Future.<Void>succeededFuture());
            }
        } else {
            // 服务器未启动，返回失败结果
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>failedFuture("telnet term server not started"));
            }
        }
    }

    /**
     * 获取实际监听的端口号
     *
     * @return 实际监听的端口
     */
    public int actualPort() {
        return bootstrap.getPort();
    }
}
