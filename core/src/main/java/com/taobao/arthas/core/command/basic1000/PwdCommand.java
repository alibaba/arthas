package com.taobao.arthas.core.command.basic1000;

import java.io.File;

import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

@Name("pwd")
@Summary("Return working directory name")
public class PwdCommand extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        String path = new File("").getAbsolutePath();
        process.appendResult(new PwdModel(path));
        process.end();
    }
}
