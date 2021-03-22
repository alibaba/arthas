package com.taobao.arthas.core.shell.term.impl.httptelnet;

import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import com.taobao.arthas.core.shell.term.impl.Helper;
import com.taobao.arthas.core.shell.term.impl.TermImpl;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

/**
 * both suport http/telnet
 * 
 * @author hengyunabc 2019-11-04
 *
 */
public class HttpTelnetTermServer extends TermServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpTelnetTermServer.class);

    private Handler<Term> termHandler;
    private NettyHttpTelnetTtyBootstrap bootstrap;
    private String hostIp;
    private int port;
    private long connectionTimeout;
    private EventExecutorGroup workerGroup;
    private HttpSessionManager httpSessionManager;

    public HttpTelnetTermServer(String hostIp, int port, long connectionTimeout, EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
        this.hostIp = hostIp;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.workerGroup = workerGroup;
        this.httpSessionManager = httpSessionManager;
    }

    @Override
    public TermServer termHandler(Handler<Term> handler) {
        this.termHandler = handler;
        return this;
    }

    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        // TODO: charset and inputrc from options
        bootstrap = new NettyHttpTelnetTtyBootstrap(workerGroup, httpSessionManager).setHost(hostIp).setPort(port);
        try {
            bootstrap.start(new Consumer<TtyConnection>() {
                @Override
                public void accept(final TtyConnection conn) {
                    termHandler.handle(new TermImpl(Helper.loadKeymap(), conn));
                }
            }).get(connectionTimeout, TimeUnit.MILLISECONDS);
            listenHandler.handle(Future.<TermServer>succeededFuture());
        } catch (Throwable t) {
            logger.error("Error listening to port " + port, t);
            listenHandler.handle(Future.<TermServer>failedFuture(t));
        }
        return this;
    }

    @Override
    public int actualPort() {
        return bootstrap.getPort();
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
}
