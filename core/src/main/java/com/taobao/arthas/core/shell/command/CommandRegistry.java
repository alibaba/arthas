package com.taobao.arthas.core.shell.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry that contains the commands known by a shell.<p/>
 * <p>
 * It is a mutable command resolver.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CommandRegistry implements CommandResolver {
    final ConcurrentHashMap<String, Command> commandMap = new ConcurrentHashMap<String, Command>();

    /**
     * Create a new registry.
     *
     * @return the created registry
     */
    public static CommandRegistry create() {
        return new CommandRegistry();
    }

    /**
     * Register a single command.
     */
    public CommandRegistry registerCommand(Class<? extends AnnotatedCommand> command) {
        return registerCommand(Command.create(command));
    }

    /**
     * Register a command
     *
     * @param command the command to register
     * @return a reference to this, so the API can be used fluently
     */
    public CommandRegistry registerCommand(Command command) {
        return registerCommands(Collections.singletonList(command));
    }

    /**
     * Register a list of commands.
     *
     * @param commands the commands to register
     * @return a reference to this, so the API can be used fluently
     */
    public CommandRegistry registerCommands(List<Command> commands) {
        for (Command command : commands) {
            commandMap.put(command.name(), command);
        }
        return this;
    }


    /**
     * Unregister a command.
     *
     * @param commandName the command name
     * @return a reference to this, so the API can be used fluently
     */
    public CommandRegistry unregisterCommand(String commandName) {
        commandMap.remove(commandName);
        return this;
    }

    @Override
    public List<Command> commands() {
        return new ArrayList<Command>(commandMap.values());
    }
}
