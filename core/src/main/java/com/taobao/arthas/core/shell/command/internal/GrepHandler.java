package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.middleware.cli.Argument;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;

import java.util.List;

/**
 * @author beiwei30 on 12/12/2016.
 */
public class GrepHandler extends StdoutHandler {
    public static final String NAME = "grep";

    private String keyword;
    private boolean ignoreCase;

    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);
        CommandLine commandLine = CLIs.create(NAME)
                .addOption(new Option().setShortName("i").setLongName("ignore-case").setFlag(true))
                .addArgument(new Argument().setArgName("keyword").setIndex(0))
                .parse(args);
        Boolean ignoreCase = commandLine.isFlagEnabled("ignore-case");
        String keyword = commandLine.getArgumentValue(0);
        return new GrepHandler(keyword, ignoreCase);
    }

    private GrepHandler(String keyword, boolean ignoreCase) {
        this.keyword = keyword;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public String apply(String input) {
        StringBuilder output = new StringBuilder();
        String[] lines = input.split("\n");
        for (String line : lines) {
            if (ignoreCase) {
                line = line.toLowerCase();
                keyword = keyword.toLowerCase();
            }

            if (line.contains(keyword)) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }
}
