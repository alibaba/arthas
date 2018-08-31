package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class CommandManagerCompletionHandler implements Handler<Completion> {
    private InternalCommandManager commandManager;

    public CommandManagerCompletionHandler(InternalCommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void handle(Completion completion) {
        commandManager.complete(completion);
    }
}
