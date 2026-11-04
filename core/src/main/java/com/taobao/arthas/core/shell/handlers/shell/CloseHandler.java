package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellImpl;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class CloseHandler implements Handler<Void> {
    private ShellImpl shell;

    public CloseHandler(ShellImpl shell) {
        this.shell = shell;
    }

    @Override
    public void handle(Void event) {
        shell.jobController().close(shell.closedFutureHandler());
    }
}
