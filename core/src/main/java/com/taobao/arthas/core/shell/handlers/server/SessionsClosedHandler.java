package com.taobao.arthas.core.shell.handlers.server;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class SessionsClosedHandler implements Handler<Future<Void>> {
    private final AtomicInteger count;
    private final Handler<Future<Void>> completionHandler;

    public SessionsClosedHandler(AtomicInteger count, Handler<Future<Void>> completionHandler) {
        this.count = count;
        this.completionHandler = completionHandler;
    }

    @Override
    public void handle(Future<Void> event) {
        if (count.decrementAndGet() == 0) {
            completionHandler.handle(Future.<Void>succeededFuture());
        }
    }
}
