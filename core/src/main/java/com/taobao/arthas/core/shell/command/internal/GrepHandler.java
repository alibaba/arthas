package com.taobao.arthas.core.shell.command.internal;

import java.util.List;
import java.util.regex.Pattern;

import com.taobao.arthas.core.command.basic1000.GrepCommand;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

/**
 * @author beiwei30 on 12/12/2016.
 */
public class GrepHandler extends StdoutHandler {
    public static final String NAME = "grep";

    private String keyword;
    private boolean ignoreCase;
    /**
     * select non-matching lines
     */
    private final boolean invertMatch;

    private final Pattern pattern;

    /**
     * print line number with output lines
     */
    private final boolean showLineNumber;

    private boolean trimEnd;

    /**
     * print NUM lines of leading context
     */
    private final Integer beforeLines;
    /**
     * print NUM lines of trailing context
     */
    private final Integer afterLines;

    /**
     * stop after NUM selected lines
     */
    private final Integer maxCount;

    private static CLI cli = null;

    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);

        GrepCommand grepCommand = new GrepCommand();
        if (cli == null) {
            cli = CLIConfigurator.define(GrepCommand.class);
        }
        CommandLine commandLine = cli.parse(args, true);

        try {
            CLIConfigurator.inject(commandLine, grepCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        int context = grepCommand.getContext();
        int beforeLines = grepCommand.getBeforeLines();
        int afterLines = grepCommand.getAfterLines();
        if (context > 0) {
            if (beforeLines < 1) {
                beforeLines = context;
            }
            if (afterLines < 1) {
                afterLines = context;
            }
        }
        return new GrepHandler(grepCommand.getPattern(), grepCommand.isIgnoreCase(), grepCommand.isInvertMatch(),
                        grepCommand.isRegEx(), grepCommand.isShowLineNumber(), grepCommand.isTrimEnd(), beforeLines,
                        afterLines, grepCommand.getMaxCount());
    }

    GrepHandler(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode,
                    boolean showLineNumber, boolean trimEnd, int beforeLines, int afterLines, int maxCount) {
        this.ignoreCase = ignoreCase;
        this.invertMatch = invertMatch;
        this.showLineNumber = showLineNumber;
        this.trimEnd = trimEnd;
        this.beforeLines = beforeLines > 0 ? beforeLines : 0;
        this.afterLines = afterLines > 0 ? afterLines : 0;
        this.maxCount = maxCount > 0 ? maxCount : 0;
        if (regexpMode) {
            final int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
            this.pattern = Pattern.compile(keyword, flags);
        } else {
            this.pattern = null;
        }
        this.keyword = ignoreCase ? keyword.toLowerCase() : keyword;
    }

    @Override
    public String apply(String input) {
        StringBuilder output = new StringBuilder();
        String[] lines = input.split("\n");
        int continueCount = 0;
        int lastStartPos = 0;
        int lastContinueLineNum = -1;
        int matchCount = 0;
        for (int lineNum = 0; lineNum < lines.length;) {
            String line = null;
            if (this.trimEnd) {
                line = StringUtils.stripEnd(lines[lineNum], null);
            } else {
                line = lines[lineNum];
            }
            lineNum++;

            final boolean match;
            if (pattern == null) {
                match = (ignoreCase ? line.toLowerCase() : line).contains(keyword);
            } else {
                match = pattern.matcher(line).find();
            }
            if (invertMatch != match) {
                matchCount++;
                if (beforeLines > continueCount) {
                    int n = lastContinueLineNum == -1 ? (beforeLines >= lineNum ? 1 : lineNum - beforeLines)
                                    : lineNum - beforeLines - continueCount;
                    if (n >= lastContinueLineNum || lastContinueLineNum == -1) {
                        StringBuilder beforeSb = new StringBuilder();
                        for (int i = n; i < lineNum; i++) {
                            appendLine(beforeSb, i, lines[i - 1]);
                        }
                        output.insert(lastStartPos, beforeSb);
                    }
                } // end handle before lines

                lastStartPos = output.length();
                appendLine(output, lineNum, line);

                if (afterLines > continueCount) {
                    int last = lineNum + afterLines - continueCount;
                    if (last > lines.length) {
                        last = lines.length;
                    }
                    for (int i = lineNum; i < last; i++) {
                        appendLine(output, i + 1, lines[i]);
                        lineNum++;
                        continueCount++;
                        lastStartPos = output.length();
                    }
                } // end handle afterLines

                continueCount++;
                if (maxCount > 0 && matchCount >= maxCount) {
                    break;
                }
            } else { // not match
                if (continueCount > 0) {
                    lastContinueLineNum = lineNum - 1;
                    continueCount = 0;
                }
            }
        }
        final String str = output.toString();
        return str;
    }

    protected void appendLine(StringBuilder output, int lineNum, String line) {
        if (showLineNumber) {
            output.append(lineNum).append(':');
        }
        output.append(line).append('\n');
    }
}
