package com.taobao.arthas.core.shell.handlers.server;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;
import com.taobao.arthas.core.shell.term.TermServer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class TermServerListenHandler implements Handler<Future<TermServer>> {
    private ShellServerImpl shellServer;
    private Handler<Future<Void>> listenHandler;
    private List<TermServer> toStart;
    private AtomicInteger count;
    private AtomicBoolean failed;

    public TermServerListenHandler(ShellServerImpl shellServer, Handler<Future<Void>> listenHandler, List<TermServer> toStart) {
        this.shellServer = shellServer;
        this.listenHandler = listenHandler;
        this.toStart = toStart;
        this.count = new AtomicInteger(toStart.size());
        this.failed = new AtomicBoolean();
    }

    @Override
    public void handle(Future<TermServer> ar) {
        if (ar.failed()) {
            failed.set(true);
        }

        if (count.decrementAndGet() == 0) {
            if (failed.get()) {
                listenHandler.handle(Future.<Void>failedFuture(ar.cause()));
                for (TermServer termServer : toStart) {
                    termServer.close();
                }
            } else {
                shellServer.setClosed(false);
                shellServer.setTimer();
                listenHandler.handle(Future.<Void>succeededFuture());
            }
        }
    }
}
