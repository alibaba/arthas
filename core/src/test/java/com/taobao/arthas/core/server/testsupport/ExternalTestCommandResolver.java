package com.taobao.arthas.core.server.testsupport;

import java.util.Collections;
import java.util.List;

import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;

public class ExternalTestCommandResolver implements CommandResolver {
    @Override
    public List<Command> commands() {
        return Collections.singletonList(Command.create(ExternalTestCommand.class));
    }
}
