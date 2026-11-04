package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class FutureHandler implements Handler<Void> {
    private Future future;

    public FutureHandler(Future future) {
        this.future = future;
    }

    @Override
    public void handle(Void event) {
        future.complete();
    }
}
