package com.taobao.arthas.core.util.usage;

import com.taobao.middleware.cli.Argument;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.UsageMessageFormatter;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;


import java.util.Collections;

import static com.taobao.text.ui.Element.row;
import static com.taobao.text.ui.Element.label;

/**
 * 带样式的使用信息格式化器
 *
 * 该类继承自 UsageMessageFormatter，用于将命令行使用信息格式化为带颜色的表格形式。
 * 主要用于生成命令行工具的帮助信息，提供更加美观和易读的输出格式。
 *
 * @author ralf0131 2016-12-14 22:16.
 */
public class StyledUsageFormatter extends UsageMessageFormatter {

    /**
     * 字体颜色
     * 用于设置高亮文本的字体颜色，例如设置为绿色可以使关键词更加醒目
     */
    private Color fontColor;

    /**
     * 构造函数
     *
     * @param fontColor 字体颜色，用于设置高亮文本的颜色
     */
    public StyledUsageFormatter(Color fontColor) {
        this.fontColor = fontColor;
    }

    /**
     * 生成带样式的使用信息字符串
     *
     * 静态工具方法，用于快速生成命令行工具的带颜色格式化的使用说明。
     * 默认使用绿色作为高亮颜色。
     *
     * @param cli 命令行接口对象，包含命令的定义信息
     * @param width 输出宽度限制，用于格式化表格
     * @return 格式化后的使用信息字符串，如果 cli 为 null 则返回空字符串
     */
    public static String styledUsage(CLI cli, int width) {
        // 检查 CLI 对象是否为空
        if(cli == null) {
            return "";
        }
        // 创建字符串构建器用于存储格式化结果
        StringBuilder usageBuilder = new StringBuilder();
        // 创建使用绿色作为高亮色的格式化器
        UsageMessageFormatter formatter = new StyledUsageFormatter(Color.green);
        // 设置输出宽度
        formatter.setWidth(width);
        // 使用格式化器生成使用信息
        cli.usage(usageBuilder, formatter);
        return usageBuilder.toString();
    }

    /**
     * 生成使用信息并追加到构建器中
     *
     * 该方法生成格式化的使用信息，包括：
     * 1. USAGE 部分：显示命令的使用方式
     * 2. SUMMARY 部分：显示命令的摘要信息
     * 3. DESCRIPTION 部分：显示命令的详细描述（如果有）
     * 4. OPTIONS 部分：显示所有选项和参数的说明
     *
     * @param builder 字符串构建器，用于存储生成的使用信息
     * @param prefix 命令前缀，例如命令的名称
     * @param cli 命令行接口对象，包含命令的完整定义
     */
    @Override
    public void usage(StringBuilder builder, String prefix, CLI cli) {

        // 创建表格元素，设置1列用于标签，2列用于内容，左右内边距为1
        TableElement table = new TableElement(1, 2).leftCellPadding(1).rightCellPadding(1);

        // 添加 USAGE 标题行（高亮显示）
        table.add(row().add(label("USAGE:").style(getHighlightedStyle())));
        // 添加 USAGE 内容行（计算出的使用行）
        table.add(row().add(label(computeUsageLine(prefix, cli))));
        // 添加空行分隔
        table.add(row().add(""));
        // 添加 SUMMARY 标题行（高亮显示）
        table.add(row().add(label("SUMMARY:").style(getHighlightedStyle())));
        // 添加 SUMMARY 内容行
        table.add(row().add(label("  " + cli.getSummary())));

        // 如果命令有描述信息，则添加 DESCRIPTION 部分
        if (cli.getDescription() != null) {
            // 将描述信息按换行符分割成多行
            String[] descLines = cli.getDescription().split("\\n");
            for (String line: descLines) {
                // 判断该行是否应该高亮显示（不以空格开头的行会被高亮）
                if (shouldBeHighlighted(line)) {
                    table.add(row().add(label(line).style(getHighlightedStyle())));
                } else {
                    table.add(row().add(label(line)));
                }
            }
        }

        // 如果命令有选项或参数，则添加 OPTIONS 部分
        if (!cli.getOptions().isEmpty() || !cli.getArguments().isEmpty()) {
            // 添加空行分隔
            table.add(row().add(""));
            // 添加 OPTIONS 标题行（高亮显示）
            table.row(label("OPTIONS:").style(getHighlightedStyle()));

            // 遍历并添加所有选项
            for (Option option : cli.getOptions()) {
                StringBuilder optionSb = new StringBuilder(32);

                // 处理短选项名（例如 -h）
                if (isNullOrEmpty(option.getShortName())) {
                    // 如果没有短选项名，添加3个空格占位
                    optionSb.append("   ");
                } else {
                    // 添加短选项名
                    optionSb.append('-').append(option.getShortName());
                    if (isNullOrEmpty(option.getLongName())) {
                        // 如果没有长选项名，添加空格
                        optionSb.append(' ');
                    } else {
                        // 如果有长选项名，添加逗号分隔
                        optionSb.append(',');
                    }
                }
                // 处理长选项名（例如 --help）
                if (!isNullOrEmpty(option.getLongName())) {
                    optionSb.append(" --").append(option.getLongName());
                }

                // 如果选项接受值，添加 <value> 占位符
                if (option.acceptValue()) {
                    optionSb.append(" <value>");
                }

                // 添加选项行：第一列是选项名（高亮），第二列是选项描述
                table.add(row().add(label(optionSb.toString()).style(getHighlightedStyle()))
                                .add(option.getDescription()));
            }

            // 遍历并添加所有参数
            for (Argument argument: cli.getArguments()) {
                // 添加参数行：第一列是参数名（高亮，用尖括号包围），第二列是参数描述
                table.add(row().add(label("<" + argument.getArgName() + ">").style(getHighlightedStyle()))
                        .add(argument.getDescription()));
            }
        }

        // 将表格渲染为字符串并追加到构建器中
        builder.append(RenderUtil.render(table, getWidth()));
    }

    /**
     * 获取高亮样式
     *
     * 创建一个包含粗体和指定字体颜色的组合样式，用于突出显示重要的文本内容。
     *
     * @return 包含粗体和字体颜色的样式组合对象
     */
    private Style.Composite getHighlightedStyle() {
        return Style.style(Decoration.bold, fontColor);
    }

    /**
     * 计算并生成使用行字符串
     *
     * 该方法生成一个简洁的使用行，显示命令的基本用法，包括命令名、所有选项和参数。
     * 格式通常为：prefix command-name [options] <arguments>
     *
     * @param prefix 命令前缀，可能包含路径或其他前缀信息
     * @param cli 命令行接口对象，包含命令的定义信息
     * @return 生成的使用行字符串
     */
    public String computeUsageLine(String prefix, CLI cli) {
        // 初始化字符串缓冲区
        StringBuilder buff;
        // 处理前缀：如果为空则添加2个空格，否则添加前缀和必要的空格
        if (prefix == null) {
            buff = new StringBuilder("  ");
        } else {
            buff = new StringBuilder("  ").append(prefix);
            // 如果前缀不以空格结尾，添加一个空格
            if (!prefix.endsWith(" ")) {
                buff.append(" ");
            }
        }

        // 添加命令名称
        buff.append(cli.getName()).append(" ");

        // 如果设置了选项比较器，则对选项进行排序
        if (getOptionComparator() != null) {
            Collections.sort(cli.getOptions(), getOptionComparator());
        }

        // 遍历并添加所有选项
        for (Option option : cli.getOptions()) {
            appendOption(buff, option);
            buff.append(" ");
        }

        // 遍历并添加所有参数
        for (Argument arg : cli.getArguments()) {
            // 根据参数是否必需来决定添加方式
            appendArgument(buff, arg, arg.isRequired());
            buff.append(" ");
        }

        return buff.toString();
    }

    /**
     * 判断某行文本是否应该高亮显示
     *
     * 根据行的格式来判断是否需要高亮。不以空格开头的行通常被视为标题或重要内容，
     * 需要高亮显示。
     *
     * @param line 要判断的文本行
     * @return 如果行不以空格开头返回 true（需要高亮），否则返回 false
     */
    private boolean shouldBeHighlighted(String line) {
        return !line.startsWith(" ");
    }

}
