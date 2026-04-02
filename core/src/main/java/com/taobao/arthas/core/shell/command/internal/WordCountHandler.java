package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 字数统计处理器
 * <p>
 * 该类实现了类似Linux系统中'wc'命令的功能，用于统计输出的行数。
 * 继承自StdoutHandler并实现StatisticsFunction接口，提供统计功能。
 * 当前只支持-l选项（统计行数）。
 * </p>
 *
 * @author ralf0131 2017-02-23 23:28.
 */
public class WordCountHandler extends StdoutHandler implements StatisticsFunction  {

    /** 命令名称 */
    public static final String NAME = "wc";

    /** 是否为行数统计模式 */
    private boolean lineMode;

    /** 统计结果字符串（用于错误信息） */
    private String result = null;

    /** 总行数计数器，使用AtomicInteger保证线程安全 */
    private final AtomicInteger total = new AtomicInteger(0);

    /**
     * 根据命令行token注入WordCountHandler实例
     * <p>
     * 该方法解析命令行参数，创建WordCountHandler实例。
     * 支持-l选项用于行数统计模式。
     * </p>
     *
     * @param tokens 命令行token列表
     * @return WordCountHandler实例
     */
    public static StdoutHandler inject(List<CliToken> tokens) {
        // 解析命令行参数
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);
        // 创建CLI并添加-l选项
        CommandLine commandLine = CLIs.create(NAME)
                .addOption(new Option().setShortName("l").setFlag(true))
                .parse(args);
        // 检查是否启用了行数统计模式
        boolean lineMode = commandLine.isFlagEnabled("l");
        return new WordCountHandler(lineMode);
    }

    /**
     * 私有构造函数
     *
     * @param lineMode 是否启用行数统计模式
     */
    private WordCountHandler(boolean lineMode) {
        this.lineMode = lineMode;
    }

    /**
     * 处理输入数据并统计行数
     * <p>
     * 该方法根据当前模式处理输入数据：
     * <ul>
     * <li>如果是行数模式（-l），统计输入数据的行数并累加到总数中</li>
     * <li>如果不是行数模式，设置错误提示信息</li>
     * </ul>
     * 注意：该方法返回null，表示数据不会被继续传递。
     * </p>
     *
     * @param input 输入数据
     * @return null（统计数据不继续传递）
     */
    @Override
    public String apply(String input) {
        // 如果不是行数统计模式，返回错误提示
        if (!this.lineMode) {
            // TODO the default behavior should be equivalent to `wc -l -w -c`
            result = "wc currently only support wc -l!\n";
        } else {
            // 如果输入不为空，按换行符分割并统计行数
            if (input != null && !"".equals(input.trim())) {
                total.getAndAdd(input.split("\n").length);
            }
        }

        return null;
    }

    /**
     * 获取统计结果
     * <p>
     * 返回统计的总行数。如果之前设置了错误信息（非行数模式），
     * 则返回错误信息；否则返回统计的行数。
     * </p>
     *
     * @return 统计结果字符串，包含行数和换行符
     */
    @Override
    public String result() {
        // 如果有错误信息，返回错误信息
        if (result != null) {
            return result;
        }

        // 返回统计的总行数
        return total.get() + "\n";
    }
}
