package com.taobao.arthas.common;

/**
 * 使用说明渲染工具类
 * 用于格式化命令行工具的使用说明文本，支持彩色输出
 * 可以将普通的usage文本渲染成带有颜色高亮的格式，提升可读性
 *
 * @author hengyunabc 2018-11-22
 *
 */
public class UsageRender {

    /**
     * 私有构造函数，防止实例化
     */
    private UsageRender() {
    }

    /**
     * 渲染使用说明文本
     * 如果启用了彩色输出，会将特定的行（如Usage行和标题行）渲染成绿色
     * 如果未启用彩色输出，则直接返回原始文本
     *
     * @param usage 原始的使用说明文本
     * @return 渲染后的使用说明文本，如果启用彩色输出则包含ANSI颜色代码
     */
    public static String render(String usage) {
        // 检查是否启用彩色输出
        if (AnsiLog.enableColor()) {
            // 使用StringBuilder构建渲染后的文本
            StringBuilder sb = new StringBuilder(1024);
            // 按行分割usage文本（支持Windows和Unix的换行符）
            String lines[] = usage.split("\\r?\\n");
            for (String line : lines) {
                // 如果行以"Usage: "开头，将"Usage: "部分渲染为绿色
                if (line.startsWith("Usage: ")) {
                    sb.append(AnsiLog.green("Usage: "));
                    // 添加"Usage: "之后的内容
                    sb.append(line.substring("Usage: ".length()));
                }
                // 如果行不以空格开头且以冒号结尾，说明是标题行，渲染为绿色
                else if (!line.startsWith(" ") && line.endsWith(":")) {
                    sb.append(AnsiLog.green(line));
                }
                // 普通行直接添加
                else {
                    sb.append(line);
                }
                // 添加换行符
                sb.append('\n');
            }
            return sb.toString();
        } else {
            // 未启用彩色输出，直接返回原始文本
            return usage;
        }
    }
}
