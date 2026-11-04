package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.middleware.cli.CLI;

import java.util.Collections;
import java.util.List;

public abstract class Command {

    /**
     * Create a command from a Java class, annotated with CLI annotations.
     *
     * @param clazz the class of the command
     * @return the command object
     */
    public static Command create(final Class<? extends AnnotatedCommand> clazz) {
        return new AnnotatedCommandImpl(clazz);
    }

    /**
     * @return the command name
     */
    public String name() {
        return null;
    }

    /**
     * @return the command line interface, can be null
     */
    public CLI cli() {
        return null;
    }

    /**
     * Create a new process with the passed arguments.
     *
     * @return the process handler
     */
    public abstract Handler<CommandProcess> processHandler();

    /**
     * Perform command completion, when the command is done completing it should call {@link Completion#complete(List)}
     * or {@link Completion#complete(String, boolean)} )} method to signal completion is done.
     *
     * @param completion the completion object
     */
    public void complete(Completion completion) {
        completion.complete(Collections.<String>emptyList());
    }
}
