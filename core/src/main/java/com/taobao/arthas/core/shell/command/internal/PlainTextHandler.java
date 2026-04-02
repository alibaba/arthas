package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.text.util.RenderUtil;

import java.util.List;

/**
 * 纯文本输出处理器
 * <p>
 * 该处理器负责将包含ANSI转义序列的输出转换为纯文本格式。
 * 继承自StdoutHandler，用于处理标准输出的格式化。
 * 主要用于在不需要颜色和格式化的场景下输出纯文本内容。
 * </p>
 *
 * @author beiwei30 on 20/12/2016.
 */
public class PlainTextHandler extends StdoutHandler {
    /**
     * 处理器名称常量
     * <p>
     * 用于标识该处理器的名称，在处理器注册和查找时使用。
     * </p>
     */
    public static String NAME = "plaintext";

    /**
     * 注入方法，创建PlainTextHandler实例
     * <p>
     * 该方法是一个静态工厂方法，用于从CLI token列表中创建PlainTextHandler实例。
     * 注意：当前实现中并未使用tokens参数，这是一个预留的扩展点。
     * </p>
     *
     * @param tokens CLI token列表，包含解析后的命令行token对象
     * @return 新创建的PlainTextHandler实例
     */
    public static StdoutHandler inject(List<CliToken> tokens) {
        return new PlainTextHandler();
    }

    /**
     * 处理输出字符串，将其转换为纯文本格式
     * <p>
     * 该方法接收包含ANSI转义序列的字符串，并将其转换为纯文本格式。
     * ANSI转义序列通常用于控制终端的颜色、光标位置等，但在纯文本场景下需要移除。
     * </p>
     *
     * @param s 输入字符串，可能包含ANSI转义序列
     * @return 移除所有ANSI转义序列后的纯文本字符串
     */
    @Override
    public String apply(String s) {
        return RenderUtil.ansiToPlainText(s);
    }
}
