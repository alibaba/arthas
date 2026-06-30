package com.taobao.arthas.core.server.testsupport;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

@Name("external-test")
@Summary("external test command")
public class ExternalTestCommand extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        process.end();
    }
}
