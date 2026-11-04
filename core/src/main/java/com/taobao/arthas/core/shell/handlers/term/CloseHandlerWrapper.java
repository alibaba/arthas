package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.handlers.Handler;
import io.termd.core.function.Consumer;

/**
 * @author beiwei30 on 22/11/2016.
 */
public class CloseHandlerWrapper implements Consumer<Void> {
    private final Handler<Void> handler;

    public CloseHandlerWrapper(Handler<Void> handler) {
        this.handler = handler;
    }

    @Override
    public void accept(Void v) {
        handler.handle(v);
    }
}
