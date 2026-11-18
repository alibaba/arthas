package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * @see com.taobao.arthas.core.shell.command.internal.WordCountHandler
 */
@Name("wc")
@Summary("Count lines in command output.")
@Description(Constants.EXAMPLE +
        " thread | wc -l\n" +
        " sysprop | wc --lines\n"
        + Constants.WIKI + Constants.WIKI_HOME + "wc")
public class WordCountCommand extends AnnotatedCommand {

    private boolean lineMode;

    @Option(shortName = "l", longName = "lines", flag = true)
    @Description("Count newline characters")
    public void setLineMode(boolean lineMode) {
        this.lineMode = lineMode;
    }

    public boolean isLineMode() {
        return lineMode;
    }

    @Override
    public void process(CommandProcess process) {
        process.end(-1, "The wc command is only for pipes. See 'wc --help'\n");
    }
}
