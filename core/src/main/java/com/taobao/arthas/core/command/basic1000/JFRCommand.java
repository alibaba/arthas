package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

@Name("jfr")
@Summary("Java Flight Recorder")
public class JFRCommand extends AnnotatedCommand {

    @Override
    public void process(CommandProcess process) {
        System.out.println("hello jfr");
    }
}
