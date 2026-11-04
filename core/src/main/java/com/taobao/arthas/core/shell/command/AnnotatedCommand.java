package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.middleware.cli.CLI;

import java.util.List;

/**
 * The base command class that Java annotated command should extend.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class AnnotatedCommand {

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
     * Process the command, when the command is done processing it should call the {@link CommandProcess#end()} method.
     *
     * @param process the command process
     */
    public abstract void process(CommandProcess process);

    /**
     * Perform command completion, when the command is done completing it should call {@link Completion#complete(List)}
     * or {@link Completion#complete(String, boolean)} )} method to signal completion is done.
     *
     * @param completion the completion object
     */
    public void complete(Completion completion) {
        CompletionUtils.complete(completion, this.getClass());
    }

}

