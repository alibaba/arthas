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
    protected final Integer maxCount;

    private static CLI cli = null;

    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);

        GrepCommand grepCommand = new GrepCommand();
        if (cli == null) {
            cli = CLIConfigurator.define(GrepCommand.class, true);
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
        if (grepCommand.isCount()) {
            return new GrepCountHandler(grepCommand.getPattern(), grepCommand.isIgnoreCase(),
                            grepCommand.isInvertMatch(), grepCommand.isRegEx(), grepCommand.isTrimEnd(),
                            grepCommand.getMaxCount());
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
        int lastPrintedLineNum = 0;
        int pendingAfterLines = 0;
        int matchCount = 0;
        for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
            String line = prepareLine(lines[lineNum - 1]);

            if (isSelectedLine(line)) {
                if (maxCount > 0 && matchCount >= maxCount) {
                    // trailing context of the last selected line ends at the next match
                    break;
                }
                matchCount++;
                int firstBeforeLineNum = Math.max(lastPrintedLineNum + 1, lineNum - beforeLines);
                for (int i = firstBeforeLineNum; i < lineNum; i++) {
                    appendLine(output, i, prepareLine(lines[i - 1]));
                }
                appendLine(output, lineNum, line);
                lastPrintedLineNum = lineNum;
                pendingAfterLines = afterLines;
            } else if (pendingAfterLines > 0) {
                appendLine(output, lineNum, line);
                lastPrintedLineNum = lineNum;
                pendingAfterLines--;
            }

            if (maxCount > 0 && matchCount >= maxCount && pendingAfterLines == 0) {
                break;
            }
        }
        final String str = output.toString();
        return str;
    }

    protected String prepareLine(String line) {
        if (this.trimEnd) {
            return StringUtils.stripEnd(line, null);
        }
        return line;
    }

    protected boolean isSelectedLine(String line) {
        final boolean match;
        if (pattern == null) {
            match = (ignoreCase ? line.toLowerCase() : line).contains(keyword);
        } else {
            match = pattern.matcher(line).find();
        }
        return invertMatch != match;
    }

    protected void appendLine(StringBuilder output, int lineNum, String line) {
        if (showLineNumber) {
            output.append(lineNum).append(':');
        }
        output.append(line).append('\n');
    }

    private static class GrepCountHandler extends GrepHandler implements StatisticsFunction {

        private int count;

        GrepCountHandler(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode,
                        boolean trimEnd, int maxCount) {
            super(keyword, ignoreCase, invertMatch, regexpMode, false, trimEnd, 0, 0, maxCount);
        }

        @Override
        public String apply(String input) {
            if (input == null) {
                return null;
            }

            String[] lines = input.split("\n");
            for (String rawLine : lines) {
                if (maxCount > 0 && count >= maxCount) {
                    break;
                }
                String line = prepareLine(rawLine);
                if (isSelectedLine(line)) {
                    count++;
                }
            }
            return null;
        }

        @Override
        public String result() {
            return count + "\n";
        }
    }
}
