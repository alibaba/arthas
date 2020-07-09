package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Alias for ShutdownCommand
 * @author hengyunabc 2019-07-05
 * @see ShutdownCommand
 */
@Name("stop")
@Summary("Stop/Shutdown Arthas server and exit the console.")
public class StopCommand extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        ShutdownCommand.shutdown(process);
    }
}
