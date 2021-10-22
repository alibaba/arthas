package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.EchoModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 
 * @author hengyunabc
 *
 */
@Name("echo")
@Summary("write arguments to the standard output")
@Description("\nExamples:\n" +
        "  echo 'abc'\n" +
        Constants.WIKI + Constants.WIKI_HOME + "echo")
public class EchoCommand extends AnnotatedCommand {
    private String message;

    @Argument(argName = "message", index = 0, required = false)
    @Description("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void process(CommandProcess process) {
        if (message != null) {
            process.appendResult(new EchoModel(message));
        }

        process.end();
    }

}
