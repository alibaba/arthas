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
 * Grep命令处理器
 *
 * 该类用于处理类似Unix grep命令的过滤操作，支持：
 * - 关键字搜索（支持正则表达式）
 * - 大小写敏感/不敏感匹配
 * - 反向匹配（选择不匹配的行）
 * - 显示行号
 * - 上下文行显示（匹配行前后的行）
 * - 最大匹配行数限制
 * - 行尾空白字符处理
 *
 * @author beiwei30 on 12/12/2016.
 */
public class GrepHandler extends StdoutHandler {

    // grep命令名称常量
    public static final String NAME = "grep";

    // 搜索关键字
    private String keyword;

    // 是否忽略大小写
    private boolean ignoreCase;

    /**
     * 是否反向匹配（选择不匹配的行）
     */
    private final boolean invertMatch;

    // 正则表达式模式对象（如果使用正则模式）
    private final Pattern pattern;

    /**
     * 是否显示行号
     */
    private final boolean showLineNumber;

    // 是否去除行尾空白
    private boolean trimEnd;

    /**
     * 显示匹配行之前的行数
     */
    private final Integer beforeLines;

    /**
     * 显示匹配行之后的行数
     */
    private final Integer afterLines;

    /**
     * 最大匹配行数（达到此数量后停止）
     */
    private final Integer maxCount;

    // CLI对象，用于解析命令行参数
    private static CLI cli = null;

    /**
     * 根据命令行tokens创建GrepHandler实例
     *
     * 该方法解析命令行参数，创建并配置GrepHandler对象
     *
     * @param tokens 命令行token列表
     * @return 配置好的GrepHandler实例
     */
    public static StdoutHandler inject(List<CliToken> tokens) {
        // 解析命令行参数
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);

        // 创建GrepCommand对象并注入参数
        GrepCommand grepCommand = new GrepCommand();
        // 延迟初始化CLI对象
        if (cli == null) {
            cli = CLIConfigurator.define(GrepCommand.class);
        }
        // 解析命令行
        CommandLine commandLine = cli.parse(args, true);

        try {
            // 将命令行参数注入到GrepCommand对象
            CLIConfigurator.inject(commandLine, grepCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // 获取上下文行数参数
        int context = grepCommand.getContext();
        int beforeLines = grepCommand.getBeforeLines();
        int afterLines = grepCommand.getAfterLines();
        // 如果设置了context参数，且before/after参数未设置，则使用context的值
        if (context > 0) {
            if (beforeLines < 1) {
                beforeLines = context;
            }
            if (afterLines < 1) {
                afterLines = context;
            }
        }
        // 创建并返回GrepHandler实例
        return new GrepHandler(grepCommand.getPattern(), grepCommand.isIgnoreCase(), grepCommand.isInvertMatch(),
                        grepCommand.isRegEx(), grepCommand.isShowLineNumber(), grepCommand.isTrimEnd(), beforeLines,
                        afterLines, grepCommand.getMaxCount());
    }

    /**
     * GrepHandler构造函数
     *
     * @param keyword 搜索关键字或正则表达式
     * @param ignoreCase 是否忽略大小写
     * @param invertMatch 是否反向匹配
     * @param regexpMode 是否使用正则表达式模式
     * @param showLineNumber 是否显示行号
     * @param trimEnd 是否去除行尾空白
     * @param beforeLines 显示匹配行之前的行数
     * @param afterLines 显示匹配行之后的行数
     * @param maxCount 最大匹配行数
     */
    GrepHandler(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode,
                    boolean showLineNumber, boolean trimEnd, int beforeLines, int afterLines, int maxCount) {
        this.ignoreCase = ignoreCase;
        this.invertMatch = invertMatch;
        this.showLineNumber = showLineNumber;
        this.trimEnd = trimEnd;
        // 确保行数不为负数
        this.beforeLines = beforeLines > 0 ? beforeLines : 0;
        this.afterLines = afterLines > 0 ? afterLines : 0;
        this.maxCount = maxCount > 0 ? maxCount : 0;
        // 如果使用正则表达式模式，编译正则表达式
        if (regexpMode) {
            final int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
            this.pattern = Pattern.compile(keyword, flags);
        } else {
            // 非正则模式，pattern为null
            this.pattern = null;
        }
        // 根据ignoreCase设置关键字
        this.keyword = ignoreCase ? keyword.toLowerCase() : keyword;
    }

    /**
     * 处理输入文本，应用grep过滤逻辑
     *
     * 该方法是GrepHandler的核心，实现以下功能：
     * 1. 将输入文本按行分割
     * 2. 对每一行进行匹配检查
     * 3. 根据匹配结果和配置决定是否输出该行
     * 4. 处理上下文行（before/after）
     * 5. 控制最大匹配行数
     *
     * @param input 输入文本
     * @return 过滤后的文本
     */
    @Override
    public String apply(String input) {
        // 创建输出缓冲区
        StringBuilder output = new StringBuilder();
        // 将输入按行分割
        String[] lines = input.split("\n");
        // 初始化计数器
        int continueCount = 0;      // 连续匹配计数（用于处理afterLines）
        int lastStartPos = 0;       // 上次插入位置（用于处理beforeLines）
        int lastContinueLineNum = -1; // 上次连续行号
        int matchCount = 0;         // 匹配行计数（用于maxCount）

        // 遍历所有行
        for (int lineNum = 0; lineNum < lines.length;) {
            String line = null;
            // 根据trimEnd配置处理行尾空白
            if (this.trimEnd) {
                line = StringUtils.stripEnd(lines[lineNum], null);
            } else {
                line = lines[lineNum];
            }
            lineNum++;

            // 判断当前行是否匹配
            final boolean match;
            if (pattern == null) {
                // 非正则模式：使用字符串包含检查
                match = (ignoreCase ? line.toLowerCase() : line).contains(keyword);
            } else {
                // 正则模式：使用正则表达式匹配
                match = pattern.matcher(line).find();
            }

            // 根据invertMatch和match决定是否处理该行
            if (invertMatch != match) {
                // 匹配成功（或反向匹配成功）
                matchCount++;

                // 处理beforeLines：显示匹配行之前的行
                if (beforeLines > continueCount) {
                    // 计算需要显示的起始行
                    int n = lastContinueLineNum == -1 ? (beforeLines >= lineNum ? 1 : lineNum - beforeLines)
                                    : lineNum - beforeLines - continueCount;
                    // 如果起始行有效
                    if (n >= lastContinueLineNum || lastContinueLineNum == -1) {
                        StringBuilder beforeSb = new StringBuilder();
                        // 添加beforeLines范围内的行
                        for (int i = n; i < lineNum; i++) {
                            appendLine(beforeSb, i, lines[i - 1]);
                        }
                        // 在上次插入位置插入beforeLines内容
                        output.insert(lastStartPos, beforeSb);
                    }
                } // end handle before lines

                // 记录当前插入位置
                lastStartPos = output.length();
                // 添加当前匹配行
                appendLine(output, lineNum, line);

                // 处理afterLines：显示匹配行之后的行
                if (afterLines > continueCount) {
                    // 计算afterLines的结束位置
                    int last = lineNum + afterLines - continueCount;
                    // 确保不超过总行数
                    if (last > lines.length) {
                        last = lines.length;
                    }
                    // 添加afterLines范围内的行
                    for (int i = lineNum; i < last; i++) {
                        appendLine(output, i + 1, lines[i]);
                        lineNum++;
                        continueCount++;
                        lastStartPos = output.length();
                    }
                } // end handle afterLines

                // 增加连续计数
                continueCount++;

                // 检查是否达到最大匹配行数
                if (maxCount > 0 && matchCount >= maxCount) {
                    break;
                }
            } else {
                // 不匹配：重置连续计数
                if (continueCount > 0) {
                    lastContinueLineNum = lineNum - 1;
                    continueCount = 0;
                }
            }
        }
        // 返回过滤后的字符串
        final String str = output.toString();
        return str;
    }

    /**
     * 向输出缓冲区添加一行
     *
     * 根据showLineNumber配置决定是否在行首添加行号
     *
     * @param output 输出缓冲区
     * @param lineNum 行号
     * @param line 行内容
     */
    protected void appendLine(StringBuilder output, int lineNum, String line) {
        if (showLineNumber) {
            // 如果需要显示行号，在行首添加"行号:"
            output.append(lineNum).append(':');
        }
        // 添加行内容和换行符
        output.append(line).append('\n');
    }
}
