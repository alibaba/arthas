package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.handlers.Handler;
import io.termd.core.function.Consumer;
import io.termd.core.util.Helper;

/**
 * @author beiwei30 on 22/11/2016.
 */
public class StdinHandlerWrapper implements Consumer<int[]> {
    private final Handler<String> handler;

    public StdinHandlerWrapper(Handler<String> handler) {
        this.handler = handler;
    }

    @Override
    public void accept(int[] codePoints) {
        handler.handle(Helper.fromCodePoints(codePoints));
    }
}
