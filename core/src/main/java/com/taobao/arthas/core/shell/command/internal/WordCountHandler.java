package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.command.basic1000.WordCountCommand;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ralf0131 2017-02-23 23:28.
 */
public class WordCountHandler extends StdoutHandler implements StatisticsFunction  {

    public static final String NAME = "wc";

    private static final CLI cli = CLIConfigurator.define(WordCountCommand.class, true);

    private boolean lineMode;

    private String result = null;
    private final AtomicLong total = new AtomicLong(0);

    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);
        CommandLine commandLine = cli.parse(args);
        boolean lineMode = commandLine.isFlagEnabled("l");
        return new WordCountHandler(lineMode);
    }

    private WordCountHandler(boolean lineMode) {
        this.lineMode = lineMode;
    }

    @Override
    public String apply(String input) {
        if (!this.lineMode) {
            // TODO the default behavior should be equivalent to `wc -l -w -c`
            result = "wc currently only support wc -l!\n";
        } else {
            if (input != null) {
                int lines = 0;
                for (int i = 0; i < input.length(); i++) {
                    if (input.charAt(i) == '\n') {
                        lines++;
                    }
                }
                total.addAndGet(lines);
            }
        }

        return null;
    }

    @Override
    public String result() {
        if (result != null) {
            return result;
        }

        return total.get() + "\n";
    }
}
