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
 * Grep命令类
 * 用于管道过滤，类似于Unix/Linux的grep命令
 * 该命令仅用于管道处理，实际过滤逻辑由GrepHandler实现
 *
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
    // 匹配模式字符串
    private String pattern;
    // 是否忽略大小写
    private boolean ignoreCase;

    /**
     * 是否反向匹配（选择不匹配的行）
     */
    private boolean invertMatch;

    // 是否使用正则表达式
    private boolean isRegEx = false;

    /**
     * 是否显示行号
     */
    private boolean showLineNumber = false;

    // 是否去除行尾空白
    private boolean trimEnd;

    /**
     * 显示匹配行之前的行数
     */
    private int beforeLines;

    /**
     * 显示匹配行之后的行数
     */
    private int afterLines;

    /**
     * 显示匹配行前后的行数
     */
    private int context;

    /**
     * 最大匹配行数限制
     */
    private int maxCount;

    /**
     * 设置匹配模式
     *
     * @param pattern 匹配模式字符串
     */
    @Argument(index = 0, argName = "pattern", required = true)
    @Description("Pattern")
    public void setOptionName(String pattern) {
        this.pattern = pattern;
    }

    /**
     * 设置是否启用正则表达式匹配
     *
     * @param regEx 是否启用正则表达式
     */
    @Option(shortName = "e", longName = "regex", flag = true)
    @Description("Enable regular expression to match")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置是否忽略大小写
     *
     * @param ignoreCase 是否忽略大小写
     */
    @Option(shortName = "i", longName = "ignore-case", flag = true)
    @Description("Perform case insensitive matching.  By default, grep is case sensitive.")
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     * 设置是否反向匹配（选择不匹配的行）
     *
     * @param invertMatch 是否反向匹配
     */
    @Option(shortName = "v", longName = "invert-match", flag = true)
    @Description("Select non-matching lines")
    public void setInvertMatch(boolean invertMatch) {
        this.invertMatch = invertMatch;
    }

    /**
     * 设置是否显示行号
     *
     * @param showLineNumber 是否显示行号
     */
    @Option(shortName = "n", longName = "line-number", flag = true)
    @Description("Print line number with output lines")
    public void setShowLineNumber(boolean showLineNumber) {
        this.showLineNumber = showLineNumber;
    }

    /**
     * 设置是否去除行尾空白
     *
     * @param trimEnd 是否去除行尾空白，默认为true
     */
    @Option(longName = "trim-end", flag = false)
    @DefaultValue("true")
    @Description("Remove whitespaces at the end of the line, default value true")
    public void setTrimEnd(boolean trimEnd) {
        this.trimEnd = trimEnd;
    }

    /**
     * 设置显示匹配行之前的行数
     *
     * @param beforeLines 之前的行数
     */
    @Option(shortName = "B", longName = "before-context")
    @Description("Print NUM lines of leading context)")
    public void setBeforeLines(int beforeLines) {
        this.beforeLines = beforeLines;
    }

    /**
     * 设置显示匹配行之后的行数
     *
     * @param afterLines 之后的行数
     */
    @Option(shortName = "A", longName = "after-context")
    @Description("Print NUM lines of trailing context)")
    public void setAfterLines(int afterLines) {
        this.afterLines = afterLines;
    }

    /**
     * 设置显示匹配行前后的行数
     *
     * @param context 前后的行数
     */
    @Option(shortName = "C", longName = "context")
    @Description("Print NUM lines of output context)")
    public void setContext(int context) {
        this.context = context;
    }

    /**
     * 设置最大匹配行数限制
     *
     * @param maxCount 最大匹配行数
     */
    @Option(shortName = "m", longName = "max-count")
    @Description("stop after NUM selected lines)")
    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    /**
     * 获取匹配模式
     *
     * @return 匹配模式字符串
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * 设置匹配模式
     *
     * @param pattern 匹配模式字符串
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * 是否忽略大小写
     *
     * @return 是否忽略大小写
     */
    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    /**
     * 是否反向匹配
     *
     * @return 是否反向匹配
     */
    public boolean isInvertMatch() {
        return invertMatch;
    }

    /**
     * 是否使用正则表达式
     *
     * @return 是否使用正则表达式
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    /**
     * 是否显示行号
     *
     * @return 是否显示行号
     */
    public boolean isShowLineNumber() {
        return showLineNumber;
    }

    /**
     * 是否去除行尾空白
     *
     * @return 是否去除行尾空白
     */
    public boolean isTrimEnd() {
        return trimEnd;
    }

    /**
     * 获取匹配行之前的行数
     *
     * @return 之前的行数
     */
    public int getBeforeLines() {
        return beforeLines;
    }

    /**
     * 获取匹配行之后的行数
     *
     * @return 之后的行数
     */
    public int getAfterLines() {
        return afterLines;
    }

    /**
     * 获取匹配行前后的行数
     *
     * @return 前后的行数
     */
    public int getContext() {
        return context;
    }

    /**
     * 获取最大匹配行数
     *
     * @return 最大匹配行数
     */
    public int getMaxCount() {
        return maxCount;
    }

    /**
     * 处理命令执行
     * grep命令仅用于管道，直接结束处理并提示用户查看帮助
     *
     * @param process 命令处理进程对象
     */
    @Override
    public void process(CommandProcess process) {
        // grep命令仅用于管道处理，直接返回错误信息
        process.end(-1, "The grep command only for pipes. See 'grep --help'\n");
    }
}
