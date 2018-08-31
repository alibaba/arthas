package com.taobao.arthas.core.shell.system.impl;

import com.taobao.arthas.core.command.BuiltinCommandPack;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class InternalCommandManager {

    private final List<CommandResolver> resolvers;

    public InternalCommandManager(CommandResolver... resolvers) {
        this.resolvers = Arrays.asList(resolvers);
    }

    public InternalCommandManager(List<CommandResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public List<CommandResolver> getResolvers() {
        return resolvers;
    }

    public Command getCommand(String commandName) {
        Command command = null;
        for (CommandResolver resolver : resolvers) {
            // 内建命令在ShellLineHandler里提前处理了，所以这里不需要再查找内建命令
            if (resolver instanceof BuiltinCommandPack) {
                command = getCommand(resolver, commandName);
                if (command != null) {
                    break;
                }
            }
        }
        return command;
    }

    /**
     * Perform completion, the completion argument will be notified of the completion progress.
     *
     * @param completion the completion object
     */
    public void complete(final Completion completion) {
        List<CliToken> lineTokens = completion.lineTokens();
        int index = findLastPipe(lineTokens);
        LinkedList<CliToken> tokens = new LinkedList<CliToken>(lineTokens.subList(index + 1, lineTokens.size()));

        // Remove any leading white space
        while (tokens.size() > 0 && tokens.getFirst().isBlank()) {
            tokens.removeFirst();
        }

        // > 1 means it's a text token followed by something else
        if (tokens.size() > 1) {
            completeSingleCommand(completion, tokens);
        } else {
            completeCommands(completion, tokens);
        }
    }

    private void completeCommands(Completion completion, LinkedList<CliToken> tokens) {
        String prefix = tokens.size() > 0 ? tokens.getFirst().value() : "";
        List<String> names = new LinkedList<String>();
        for (CommandResolver resolver : resolvers) {
            for (Command command : resolver.commands()) {
                String name = command.name();
                boolean hidden = command.cli() != null && command.cli().isHidden();
                if (name.startsWith(prefix) && !names.contains(name) && !hidden) {
                    names.add(name);
                }
            }
        }
        if (names.size() == 1) {
            completion.complete(names.get(0).substring(prefix.length()), true);
        } else {
            String commonPrefix = CompletionUtils.findLongestCommonPrefix(names);
            if (commonPrefix.length() > prefix.length()) {
                completion.complete(commonPrefix.substring(prefix.length()), false);
            } else {
                completion.complete(names);
            }
        }
    }

    private void completeSingleCommand(Completion completion, LinkedList<CliToken> tokens) {
        ListIterator<CliToken> it = tokens.listIterator();
        while (it.hasNext()) {
            CliToken ct = it.next();
            it.remove();
            if (ct.isText()) {
                final List<CliToken> newTokens = new ArrayList<CliToken>();
                while (it.hasNext()) {
                    newTokens.add(it.next());
                }
                StringBuilder tmp = new StringBuilder();
                for (CliToken token : newTokens) {
                    tmp.append(token.raw());
                }
                final String line = tmp.toString();
                for (CommandResolver resolver : resolvers) {
                    Command command = getCommand(resolver, ct.value());
                    if (command != null) {
                        command.complete(new CommandCompletion(completion, line, newTokens));
                        return;
                    }
                }
                completion.complete(Collections.<String>emptyList());
            }
        }
    }

    private static Command getCommand(CommandResolver commandResolver, String name) {
        List<Command> commands = commandResolver.commands();
        if (commands == null || commands.isEmpty()) {
            return null;
        }

        for (Command command : commands) {
            if (name.equals(command.name())) {
                return command;
            }
        }
        return null;
    }

    private static int findLastPipe(List<CliToken> lineTokens) {
        int index = -1;
        for (int i = 0; i < lineTokens.size(); i++) {
            if ("|".equals(lineTokens.get(i).value())) {
                index = i;
            }
        }
        return index;
    }
}
