package com.taobao.arthas.core.shell.cli;

/**
 * CLI令牌接口
 *
 * 该接口定义了命令行令牌的基本行为。
 * 令牌是命令行字符串解析后的基本单位，可以是文本或空白字符。
 */
public interface CliToken {
    /**
     * 获取令牌的值
     *
     * 返回处理后的令牌值，已经进行了转义字符处理
     *
     * @return 令牌值
     */
    String value();

    /**
     * 获取原始令牌值
     *
     * 返回未经处理的原始令牌值，可能包含未转义的字符。
     * 例如，原始值可能是 {@literal "ab\"cd"}，包含转义字符
     *
     * @return 原始令牌值
     */
    String raw();

    /**
     * 判断是否为文本令牌
     *
     * 文本令牌是指包含实际字符内容的令牌
     *
     * @return 如果是文本令牌返回true，否则返回false
     */
    boolean isText();

    /**
     * 判断是否为空白令牌
     *
     * 空白令牌是指只包含空格、制表符等空白字符的令牌
     *
     * @return 如果是空白令牌返回true，否则返回false
     */
    boolean isBlank();
}
