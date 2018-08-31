package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.impl.CommandBuilderImpl;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.middleware.cli.CLI;

/**
 * command builder
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class CommandBuilder {

    /**
     * Create a new commmand builder, the command is responsible for managing the options and arguments via the
     * {@link CommandProcess#args() arguments}.
     *
     * @param name the command name
     * @return the command
     */
    public static CommandBuilder command(String name) {
        return new CommandBuilderImpl(name, null);
    }

    /**
     * Create a new commmand with its {@link CLI} descriptor. This command can then retrieve the parsed
     * {@link CommandProcess#commandLine()} when it executes to know get the command arguments and options.
     *
     * @param cli the cli to use
     * @return the command
     */
    public static CommandBuilder command(CLI cli) {
        return new CommandBuilderImpl(cli.getName(), cli);
    }

    /**
     * Set the command process handler, the process handler is called when the command is executed.
     *
     * @param handler the process handler
     * @return this command object
     */
    public abstract CommandBuilder processHandler(Handler<CommandProcess> handler);

    /**
     * Set the command completion handler, the completion handler when the user asks for contextual command line
     * completion, usually hitting the <i>tab</i> key.
     *
     * @param handler the completion handler
     * @return this command object
     */
    public abstract CommandBuilder completionHandler(Handler<Completion> handler);

    /**
     * Build the command
     *
     * @return the built command
     */
    public abstract Command build();

}
