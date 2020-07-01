package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * @see com.taobao.arthas.core.shell.command.internal.GrepHandler
 */
@Name("grep")
@Summary("grep command for pipes." )
@Description(Constants.EXAMPLE +
        " sysprop | grep java \n" +
        " sysprop | grep java -n\n" +
        " sysenv | grep -v JAVA\n" +
        " sysenv | grep -e \"(?i)(JAVA|sun)\" -m 3  -C 2\n" +
        " sysenv | grep JAVA -A2 -B3\n" +
        " thread | grep -m 10 -e  \"TIMED_WAITING|WAITING\"\n"
        + Constants.WIKI + Constants.WIKI_HOME + "grep")
public class GrepCommand extends AnnotatedCommand {
    private String pattern;
    private boolean ignoreCase;

    /**
     * select non-matching lines
     */
    private boolean invertMatch;

    private boolean isRegEx = false;

    /**
     * print line number with output lines
     */
    private boolean showLineNumber = false;

    private boolean trimEnd;

    /**
     * print NUM lines of leading context
     */
    private int beforeLines;

    /**
     * print NUM lines of trailing context
     */
    private int afterLines;

    /**
     * print NUM lines of output context
     */
    private int context;

    /**
     * stop after NUM selected lines
     */
    private int maxCount;

    @Argument(index = 0, argName = "pattern", required = true)
    @Description("Pattern")
    public void setOptionName(String pattern) {
        this.pattern = pattern;
    }

    @Option(shortName = "e", longName = "regex", flag = true)
    @Description("Enable regular expression to match")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "i", longName = "ignore-case", flag = true)
    @Description("Perform case insensitive matching.  By default, grep is case sensitive.")
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    @Option(shortName = "v", longName = "invert-match", flag = true)
    @Description("Select non-matching lines")
    public void setInvertMatch(boolean invertMatch) {
        this.invertMatch = invertMatch;
    }

    @Option(shortName = "n", longName = "line-number", flag = true)
    @Description("Print line number with output lines")
    public void setShowLineNumber(boolean showLineNumber) {
        this.showLineNumber = showLineNumber;
    }

    @Option(longName = "trim-end", flag = false)
    @DefaultValue("true")
    @Description("Remove whitespaces at the end of the line, default value true")
    public void setTrimEnd(boolean trimEnd) {
        this.trimEnd = trimEnd;
    }

    @Option(shortName = "B", longName = "before-context")
    @Description("Print NUM lines of leading context)")
    public void setBeforeLines(int beforeLines) {
        this.beforeLines = beforeLines;
    }

    @Option(shortName = "A", longName = "after-context")
    @Description("Print NUM lines of trailing context)")
    public void setAfterLines(int afterLines) {
        this.afterLines = afterLines;
    }

    @Option(shortName = "C", longName = "context")
    @Description("Print NUM lines of output context)")
    public void setContext(int context) {
        this.context = context;
    }

    @Option(shortName = "m", longName = "max-count")
    @Description("stop after NUM selected lines)")
    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public boolean isInvertMatch() {
        return invertMatch;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public boolean isShowLineNumber() {
        return showLineNumber;
    }

    public boolean isTrimEnd() {
        return trimEnd;
    }

    public int getBeforeLines() {
        return beforeLines;
    }

    public int getAfterLines() {
        return afterLines;
    }

    public int getContext() {
        return context;
    }

    public int getMaxCount() {
        return maxCount;
    }

    @Override
    public void process(CommandProcess process) {
        process.end(-1, "The grep command only for pipes. See 'grep --help'\n");
    }
}
