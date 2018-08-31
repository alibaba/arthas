package com.taobao.arthas.core.shell.handlers.command;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * @author ralf0131 2017-01-09 13:23.
 */
public class CommandInterruptHandler implements Handler<Void> {

    private CommandProcess process;

    public CommandInterruptHandler(CommandProcess process) {
        this.process = process;
    }

    @Override
    public void handle(Void event) {
        process.end();
        process.session().unLock();
    }
}
