package com.taobao.arthas.core.shell.system.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.taobao.arthas.core.command.BuiltinCommandPack;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.command.ShellInternalCommandResolver;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.NoOpHandler;

public class InternalCommandManagerTest {

    @Test
    public void testGetCommandSupportsBuiltinAndExternalResolver() {
        InternalCommandManager commandManager = new InternalCommandManager(Arrays.<CommandResolver>asList(
                        new InternalOnlyResolver("jobs"), new BuiltinCommandPack(Collections.<String>emptyList()),
                        new StaticCommandResolver("external-test")));

        assertThat(commandManager.getCommand("help")).isNotNull();
        assertThat(commandManager.getCommand("external-test")).isNotNull();
        assertThat(commandManager.getCommand("jobs")).isNull();
    }

    private static class InternalOnlyResolver implements ShellInternalCommandResolver {
        private final List<Command> commands;

        InternalOnlyResolver(String... commandNames) {
            this.commands = Arrays.stream(commandNames).map(StaticCommand::new).collect(java.util.stream.Collectors.toList());
        }

        @Override
        public List<Command> commands() {
            return commands;
        }
    }

    private static class StaticCommandResolver implements CommandResolver {
        private final List<Command> commands;

        StaticCommandResolver(String... commandNames) {
            this.commands = Arrays.stream(commandNames).map(StaticCommand::new).collect(java.util.stream.Collectors.toList());
        }

        @Override
        public List<Command> commands() {
            return commands;
        }
    }

    private static class StaticCommand extends Command {
        private final String name;
        private final Handler<CommandProcess> handler = new NoOpHandler<CommandProcess>();

        StaticCommand(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Handler<CommandProcess> processHandler() {
            return handler;
        }
    }
}
