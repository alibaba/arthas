package com.taobao.arthas.core.shell.term.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import com.taobao.arthas.core.shell.term.impl.http.NettyWebsocketTtyBootstrap;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

import java.util.concurrent.TimeUnit;

/**
 * HTTP终端服务器
 * 提供基于WebSocket的HTTP终端连接服务
 *
 * @author beiwei30 on 18/11/2016.
 */
public class HttpTermServer extends TermServer {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(HttpTermServer.class);

    /**
     * 终端处理器
     */
    private Handler<Term> termHandler;

    /**
     * Netty WebSocket引导启动类
     */
    private NettyWebsocketTtyBootstrap bootstrap;

    /**
     * 主机IP地址
     */
    private String hostIp;

    /**
     * 监听端口号
     */
    private int port;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectionTimeout;

    /**
     * Netty事件执行器组
     */
    private EventExecutorGroup workerGroup;

    /**
     * HTTP会话管理器
     */
    private HttpSessionManager httpSessionManager;

    /**
     * 构造HTTP终端服务器
     *
     * @param hostIp             主机IP地址
     * @param port               监听端口号
     * @param connectionTimeout  连接超时时间（毫秒）
     * @param workerGroup        Netty事件执行器组
     * @param httpSessionManager HTTP会话管理器
     */
    public HttpTermServer(String hostIp, int port, long connectionTimeout, EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
        this.hostIp = hostIp;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.workerGroup = workerGroup;
        this.httpSessionManager = httpSessionManager;
    }

    /**
     * 设置终端处理器
     *
     * @param handler 终端处理器
     * @return 当前TermServer实例，支持链式调用
     */
    @Override
    public TermServer termHandler(Handler<Term> handler) {
        this.termHandler = handler;
        return this;
    }

    /**
     * 启动服务器并开始监听
     *
     * @param listenHandler 监听完成的回调处理器
     * @return 当前TermServer实例，支持链式调用
     */
    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        // TODO: 从选项中获取字符集和inputrc配置
        bootstrap = new NettyWebsocketTtyBootstrap(workerGroup, httpSessionManager).setHost(hostIp).setPort(port);
        try {
            // 启动WebSocket服务器，并设置连接回调处理器
            bootstrap.start(new Consumer<TtyConnection>() {
                @Override
                public void accept(final TtyConnection conn) {
                    // 当有新连接时，创建TermImpl实例并交给termHandler处理
                    termHandler.handle(new TermImpl(Helper.loadKeymap(), conn));
                }
            }).get(connectionTimeout, TimeUnit.MILLISECONDS);
            // 监听成功，返回成功结果
            listenHandler.handle(Future.<TermServer>succeededFuture());
        } catch (Throwable t) {
            // 监听失败，记录错误日志并返回失败结果
            logger.error("Error listening to port " + port, t);
            listenHandler.handle(Future.<TermServer>failedFuture(t));
        }
        return this;
    }

    /**
     * 获取实际监听的端口号
     * 当端口设置为0时，系统会自动分配一个可用端口，此方法用于获取实际分配的端口号
     *
     * @return 实际监听的端口号
     */
    @Override
    public int actualPort() {
        return bootstrap.getPort();
    }

    /**
     * 关闭服务器（无回调）
     */
    @Override
    public void close() {
        close(null);
    }

    /**
     * 关闭服务器
     *
     * @param completionHandler 关闭完成的回调处理器
     */
    @Override
    public void close(Handler<Future<Void>> completionHandler) {
        if (bootstrap != null) {
            // 停止WebSocket服务器
            bootstrap.stop();
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>succeededFuture());
            }
        } else {
            // 服务器未启动
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>failedFuture("telnet term server not started"));
            }
        }
    }
}
