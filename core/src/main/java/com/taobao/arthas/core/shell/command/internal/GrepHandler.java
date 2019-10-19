package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.middleware.cli.Argument;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author beiwei30 on 12/12/2016.
 */
public class GrepHandler extends StdoutHandler {
    public static final String NAME = "grep";

    private String keyword;
    private boolean ignoreCase;
    // -v, --invert-match        select non-matching lines
    private final boolean invertMatch;
    //-e, --regexp=PATTERN      use PATTERN for matching
    private final Pattern pattern;

    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);
        CommandLine commandLine = CLIs.create(NAME)
                .addOption(new Option().setShortName("i").setLongName("ignore-case").setFlag(true))
                .addOption(new Option().setShortName("v").setLongName("invert-match").setFlag(true))
                .addOption(new Option().setShortName("e").setLongName("regexp").setFlag(true))
                .addArgument(new Argument().setArgName("keyword").setIndex(0))
                .parse(args);
        Boolean ignoreCase = commandLine.isFlagEnabled("ignore-case");
        String keyword = commandLine.getArgumentValue(0);
        final boolean invertMatch = commandLine.isFlagEnabled("invert-match");
        final boolean regexpMode = commandLine.isFlagEnabled("regexp");
        return new GrepHandler(keyword, ignoreCase, invertMatch, regexpMode);
    }

    private GrepHandler(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode) {
        this.ignoreCase = ignoreCase;
        this.invertMatch = invertMatch;
        if (regexpMode) {
           final int flags = ignoreCase ?  Pattern.CASE_INSENSITIVE : 0;
           this.pattern = Pattern.compile(keyword, flags);
        } else {
           this.pattern = null;
        }
        this.keyword = ignoreCase ?  keyword.toLowerCase() : keyword;
    }

    @Override
    public String apply(String input) {
        StringBuilder output = new StringBuilder();
        String[] lines = input.split("\n");
        for (String line : lines) {
             final boolean match;
            if (pattern == null) {
              match = (ignoreCase ?  line.toLowerCase() : line).contains(keyword);
            } else {
              match = pattern.matcher(line).find();
            }
            if (invertMatch ? !match : match) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }
}
