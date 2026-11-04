package com.taobao.arthas.core.shell.handlers.server;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class SessionClosedHandler implements Handler<Future<Void>> {
    private ShellServerImpl shellServer;
    private final ShellImpl session;

    public SessionClosedHandler(ShellServerImpl shellServer, ShellImpl session) {
        this.shellServer = shellServer;
        this.session = session;
    }

    @Override
    public void handle(Future<Void> ar) {
        shellServer.removeSession(session);
    }
}
