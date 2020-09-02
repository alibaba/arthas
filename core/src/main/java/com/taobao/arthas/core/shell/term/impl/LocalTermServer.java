package com.taobao.arthas.core.shell.term.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import com.taobao.arthas.core.shell.term.impl.local.LocalTtyServerBootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

import java.util.concurrent.TimeUnit;

/**
 * @author gongdewei 2020/9/1
 */
public class LocalTermServer extends TermServer {

    private static final Logger logger = LoggerFactory.getLogger(LocalTermServer.class);

    private Handler<Term> termHandler;
    private LocalTtyServerBootstrap bootstrap;
    private String localAddr;
    private long connectionTimeout;
    private EventExecutorGroup workerGroup;

    public LocalTermServer(String localAddr, long connectionTimeout, EventExecutorGroup workerGroup) {
        this.localAddr = localAddr;
        this.connectionTimeout = connectionTimeout;
        this.workerGroup = workerGroup;
    }

    @Override
    public TermServer termHandler(Handler<Term> handler) {
        this.termHandler = handler;
        return this;
    }

    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        // TODO: charset and inputrc from options
        bootstrap = new LocalTtyServerBootstrap(workerGroup).setAddr(localAddr);
        try {
            bootstrap.start(new Consumer<TtyConnection>() {
                @Override
                public void accept(final TtyConnection conn) {
                    termHandler.handle(new TermImpl(Helper.loadKeymap(), conn));
                }
            }).get(connectionTimeout, TimeUnit.MILLISECONDS);
            listenHandler.handle(Future.<TermServer>succeededFuture());
            logger.info("local term server is started.");
        } catch (Throwable t) {
            logger.error("start local term server error" , t);
            listenHandler.handle(Future.<TermServer>failedFuture(t));
        }
        return this;
    }

    @Override
    public int actualPort() {
        return -1;
    }

    @Override
    public void close() {
        close(null);
    }

    @Override
    public void close(Handler<Future<Void>> completionHandler) {
        if (bootstrap != null) {
            bootstrap.stop();
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>succeededFuture());
            }
        } else {
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>failedFuture("telnet term server not started"));
            }
        }
    }

    public Channel connect(Consumer<TextWebSocketFrame> clientHandler) throws InterruptedException {
        return bootstrap.connect(clientHandler);
    }
}
