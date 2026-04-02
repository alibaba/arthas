package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.cli.impl.CliTokenImpl;

import java.util.List;

/**
 * CLI令牌工厂类
 *
 * 该类提供了创建和解析CLI令牌的静态方法。
 * 它是命令行字符串解析的工具类，用于将字符串转换为令牌列表。
 *
 * @author beiwei30 on 09/11/2016.
 */
public class CliTokens {
    /**
     * 创建文本令牌
     *
     * 根据给定的文本内容创建一个文本类型的令牌。
     * 文本令牌的isText()方法会返回true。
     *
     * @param text 文本内容
     * @return 文本令牌对象
     */
    public static CliToken createText(String text) {
        return new CliTokenImpl(true, text, text);
    }

    /**
     * 创建空白令牌
     *
     * 根据给定的空白字符创建一个空白类型的令牌。
     * 空白令牌的isText()方法会返回false。
     *
     * @param blank 空白字符值
     * @return 空白令牌对象
     */
    public static CliToken createBlank(String blank) {
        return new CliTokenImpl(false, blank, blank);
    }

    /**
     * 将字符串解析为令牌列表
     *
     * 对给定的字符串进行词法分析，将其分解为一系列令牌。
     * 解析过程会处理空白字符、转义字符和引号等特殊字符。
     *
     * @param s 要解析的字符串
     * @return 令牌列表
     */
    public static List<CliToken> tokenize(String s) {
        return CliTokenImpl.tokenize(s);
    }
}
