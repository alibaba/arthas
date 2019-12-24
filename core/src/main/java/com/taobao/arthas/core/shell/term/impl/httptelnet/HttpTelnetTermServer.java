package com.taobao.arthas.core.shell.term.impl.httptelnet;

import java.util.concurrent.TimeUnit;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import com.taobao.arthas.core.shell.term.impl.Helper;
import com.taobao.arthas.core.shell.term.impl.TermImpl;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

/**
 * both suport http/telnet
 * 
 * @author hengyunabc 2019-11-04
 *
 */
public class HttpTelnetTermServer extends TermServer {

    private static Logger logger = LogUtil.getArthasLogger();

    private Handler<Term> termHandler;
    private NettyHttpTelnetTtyBootstrap bootstrap;
    private String hostIp;
    private int port;
    private long connectionTimeout;

    public HttpTelnetTermServer(String hostIp, int port, long connectionTimeout) {
        this.hostIp = hostIp;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public TermServer termHandler(Handler<Term> handler) {
        this.termHandler = handler;
        return this;
    }

    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        // TODO: charset and inputrc from options
        bootstrap = new NettyHttpTelnetTtyBootstrap().setHost(hostIp).setPort(port);
        try {
            bootstrap.start(new Consumer<TtyConnection>() {
                @Override
                public void accept(final TtyConnection conn) {
                    termHandler.handle(new TermImpl(Helper.loadKeymap(), conn));
                }
            }).get(connectionTimeout, TimeUnit.MILLISECONDS);
            listenHandler.handle(Future.<TermServer>succeededFuture());
        } catch (Throwable t) {
            logger.error(null, "Error listening to port " + port, t);
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
