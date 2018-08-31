package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.handlers.Handler;
import io.termd.core.function.Consumer;
import io.termd.core.util.Vector;

/**
 * @author beiwei30 on 22/11/2016.
 */
public class SizeHandlerWrapper implements Consumer<Vector> {
    private final Handler<Void> handler;

    public SizeHandlerWrapper(Handler<Void> handler) {
        this.handler = handler;
    }

    @Override
    public void accept(Vector resize) {
        handler.handle(null);
    }
}
