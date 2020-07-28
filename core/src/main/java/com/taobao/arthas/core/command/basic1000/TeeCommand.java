package com.taobao.arthas.core.command.basic1000;


import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.*;

/**
 * @author min.yang
 */
@Name("tee")
@Summary("tee command for pipes." )
@Description(Constants.EXAMPLE +
        " sysprop | tee /path/to/logfile | grep java \n" +
        " sysprop | tee -a /path/to/logfile | grep java \n"
        + Constants.WIKI + Constants.WIKI_HOME + "tee")
public class TeeCommand extends AnnotatedCommand {

    private String filePath;
    private boolean append;

    @Argument(index = 0, argName = "file", required = false)
    @Description("File path")
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Option(shortName = "a", longName = "append", flag = true)
    @Description("Append to file")
    public void setRegEx(boolean append) {
        this.append = append;
    }

    @Override
    public void process(CommandProcess process) {
        process.end(-1, "The tee command only for pipes. See 'tee --help'");
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isAppend() {
        return append;
    }
}
