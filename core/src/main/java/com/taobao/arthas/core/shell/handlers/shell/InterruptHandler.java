package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.term.SignalHandler;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class InterruptHandler implements SignalHandler {

    private ShellImpl shell;

    public InterruptHandler(ShellImpl shell) {
        this.shell = shell;
    }

    @Override
    public boolean deliver(int key) {
        if (shell.getForegroundJob() != null) {
            return shell.getForegroundJob().interrupt();
        }
        return true;
    }
}
