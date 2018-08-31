package com.taobao.arthas.core.shell.command;

import java.util.List;

/**
 * A resolver for commands, so the shell can discover commands.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface CommandResolver {
    /**
     * @return the current commands
     */
    List<Command> commands();
}
