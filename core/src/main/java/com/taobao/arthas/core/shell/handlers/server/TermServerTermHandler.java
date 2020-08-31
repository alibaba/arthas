package com.taobao.arthas.core.shell.handlers.server;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;
import com.taobao.arthas.core.shell.term.Term;

/**
 * @author beiwei30 on 23/11/2016.
 * #### 比如TelnetTermServer。然后TelnetTermServer的listen方法会注册一个回调类，该回调类在有新的客户端连接时会调用TermServerTermHandler的handle方法处理。
 */
public class TermServerTermHandler implements Handler<Term> {
    private ShellServerImpl shellServer;

    public TermServerTermHandler(ShellServerImpl shellServer) {
        this.shellServer = shellServer;
    }

    @Override
    public void handle(Term term) {
        shellServer.handleTerm(term);
    }
}
