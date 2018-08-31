package com.taobao.arthas.core.shell.command.impl;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandBuilder;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.middleware.cli.CLI;

import java.util.Collections;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CommandBuilderImpl extends CommandBuilder {

    private final String name;
    private final CLI cli;
    private Handler<CommandProcess> processHandler;
    private Handler<Completion> completeHandler;

    public CommandBuilderImpl(String name, CLI cli) {
        this.name = name;
        this.cli = cli;
    }

    @Override
    public CommandBuilderImpl processHandler(Handler<CommandProcess> handler) {
        processHandler = handler;
        return this;
    }

    @Override
    public CommandBuilderImpl completionHandler(Handler<Completion> handler) {
        completeHandler = handler;
        return this;
    }

    @Override
    public Command build() {
        return new CommandImpl();
    }

    private class CommandImpl extends Command {
        @Override
        public String name() {
            return name;
        }

        @Override
        public CLI cli() {
            return cli;
        }

        @Override
        public Handler<CommandProcess> processHandler() {
            return processHandler;
        }

        @Override
        public void complete(final Completion completion) {
            if (completeHandler != null) {
                try {
                    completeHandler.handle(completion);
                } catch (Throwable t) {
                    completion.complete(Collections.<String>emptyList());
                }
            } else {
                super.complete(completion);
            }
        }
    }
}
