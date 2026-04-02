package com.taobao.arthas.core.shell.cli.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import io.termd.core.readline.LineStatus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 命令行接口(CLI)令牌实现类
 * <p>
 * 该类用于表示命令行中的令牌(Token)，是命令行解析的基本单元。
 * 令牌可以是文本令牌或空白令牌，用于支持命令行的分词和处理。
 * </p>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CliTokenImpl implements CliToken {

    /**
     * 标识是否为文本令牌
     * <p>true: 表示文本令牌，包含实际的命令或参数内容</p>
     * <p>false: 表示空白令牌，包含空格或制表符</p>
     */
    final boolean text;

    /**
     * 原始字符串值
     * <p>保存令牌在原始输入字符串中的形式，包含引号、转义符等原始信息</p>
     */
    final String raw;

    /**
     * 处理后的值
     * <p>经过转义、引号处理后的实际值，可直接用于命令解析</p>
     */
    final String value;

    /**
     * 构造一个CLI令牌（原始值和值相同）
     *
     * @param text 是否为文本令牌
     * @param value 令牌的值
     */
    public CliTokenImpl(boolean text, String value) {
        this(text, value, value);
    }

    /**
     * 构造一个CLI令牌（分别指定原始值和处理后的值）
     *
     * @param text 是否为文本令牌
     * @param raw 原始字符串值
     * @param value 处理后的值
     */
    public CliTokenImpl(boolean text, String raw, String value) {
        this.text = text;
        this.raw = raw;
        this.value = value;
    }

    /**
     * 判断是否为文本令牌
     *
     * @return true表示是文本令牌，false表示是空白令牌
     */
    @Override
    public boolean isText() {
        return text;
    }

    /**
     * 判断是否为空白令牌
     *
     * @return true表示是空白令牌（空格或制表符），false表示是文本令牌
     */
    @Override
    public boolean isBlank() {
        return !text;
    }

    /**
     * 获取原始字符串值
     * <p>返回包含引号、转义符等原始信息的字符串</p>
     *
     * @return 原始字符串
     */
    public String raw() {
        return raw;
    }

    /**
     * 获取处理后的值
     * <p>返回经过转义、引号处理后的实际可用值</p>
     *
     * @return 处理后的字符串值
     */
    public String value() {
        return value;
    }

    /**
     * 计算哈希码
     * <p>基于value字段的值计算哈希码</p>
     *
     * @return 哈希码值
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * 判断对象是否相等
     * <p>两个令牌相等当且仅当它们的text类型和value值都相等</p>
     *
     * @param obj 要比较的对象
     * @return true表示相等，false表示不相等
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof CliTokenImpl) {
            CliTokenImpl that = (CliTokenImpl) obj;
            return text == that.text && value.equals(that.value);
        }
        return false;
    }

    /**
     * 转换为字符串表示
     * <p>格式: CliToken[text=类型,value=值]</p>
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "CliToken[text=" + text + ",value=" + value + "]";
    }

    /**
     * 将字符串分词为令牌列表
     * <p>
     * 该方法将输入的命令行字符串分解为一系列令牌，
     * 识别出文本令牌和空白令牌，并对管道符进行特殊处理。
     * </p>
     *
     * @param s 要分词的字符串
     * @return 令牌列表
     */
    public static List<CliToken> tokenize(String s) {

        // 创建链表用于存储令牌
        List<CliToken> tokens = new LinkedList<CliToken>();

        // 执行分词操作
        tokenize(s, 0, tokens);

        // 修正管道符的处理
        tokens = correctPipeChar(tokens);
        return tokens;

    }

    /**
     * 修正管道符'|'的处理问题
     * <p>
     * 修复管道符与命令或参数连在一起的情况，将管道符分离为独立的令牌。
     * 这是为了解决 GitHub issue #1151 中报告的问题。
     * </p>
     *
     * <p>支持的场景：</p>
     * <ul>
     * <li>1) thread| grep xxx  ->  将[thread|, grep] 转换为 [thread, |, grep]</li>
     * <li>2) thread |grep xxx  ->  将[thread, |grep] 转换为 [thread, |, grep]</li>
     * </ul>
     *
     * <p>不支持的场景：</p>
     * <ul>
     * <li>3) thread|grep xxx  （管道符两侧都没有空格）</li>
     * <li>4) trace -E classA|classB methodA|methodB|grep classA （在参数中使用管道符）</li>
     * </ul>
     *
     * @param tokens 原始令牌列表
     * @return 修正后的令牌列表
     */
    private static List<CliToken> correctPipeChar(List<CliToken> tokens) {
        // 创建新的令牌列表，预分配额外空间以容纳分离出的管道符
        List<CliToken> newTokens = new ArrayList<CliToken>(tokens.size()+4);
        for (CliToken token : tokens) {
            String tokenValue = token.value();
            // 处理以'|'结尾的令牌
            if (tokenValue.length()>1 && tokenValue.endsWith("|")) {
                // 分离最后一个字符'|'
                tokenValue = tokenValue.substring(0, tokenValue.length()-1);
                String rawValue = token.raw();
                rawValue = rawValue.substring(0, rawValue.length()-1);
                // 添加去除'|'后的令牌
                newTokens.add(new CliTokenImpl(token.isText(), rawValue, tokenValue));
                // 添加独立的'|'字符令牌
                newTokens.add(new CliTokenImpl(true, "|", "|"));

            } else if (tokenValue.length()>1 && tokenValue.startsWith("|")) {
                // 处理以'|'开头的令牌
                // 添加独立的'|'字符令牌
                newTokens.add(new CliTokenImpl(true, "|", "|"));
                // 移除第一个字符'|'
                tokenValue = tokenValue.substring(1);
                String rawValue = token.raw();
                rawValue = rawValue.substring(1);
                // 添加去除'|'后的令牌
                newTokens.add(new CliTokenImpl(token.isText(), rawValue, tokenValue));
            } else {
                // 普通令牌直接添加
                newTokens.add(token);
            }
        }
        return newTokens;
    }

    /**
     * 递归分词方法
     * <p>
     * 从指定索引开始扫描字符串，识别空白字符和文本内容，
     * 将它们转换为相应的令牌并添加到构建器中。
     * </p>
     *
     * @param s 要分词的字符串
     * @param index 开始扫描的索引位置
     * @param builder 用于存储生成的令牌的列表
     */
    private static void tokenize(String s, int index, List<CliToken> builder) {
        // 遍历字符串中的每个字符
        while (index < s.length()) {
            char c = s.charAt(index);
            switch (c) {
                case ' ':
                case '\t':
                    // 处理空白字符（空格或制表符）
                    index = blankToken(s, index, builder);
                    break;
                default:
                    // 处理文本内容
                    index = textToken(s, index, builder);
                    break;
            }
        }
    }

    // TODO: 未来应该使用码点(code points)而不是字符(char)来更好地支持Unicode
    /**
     * 提取文本令牌
     * <p>
     * 从指定位置开始提取文本内容，直到遇到空白字符。
     * 该方法会处理引号和转义字符，确保正确解析带引号的字符串。
     * </p>
     *
     * @param s 原始字符串
     * @param index 开始位置
     * @param builder 用于存储生成的令牌的列表
     * @return 下一个要处理的索引位置
     */
    private static int textToken(String s, int index, List<CliToken> builder) {
        // 创建行状态对象，用于跟踪引号和转义状态
        LineStatus quoter = new LineStatus();
        // 记录令牌的起始位置
        int from = index;
        // 用于构建处理后的值
        StringBuilder value = new StringBuilder();
        // 遍历字符串直到遇到空白字符或字符串结束
        while (index < s.length()) {
            char c = s.charAt(index);
            // 更新引号状态
            quoter.accept(c);
            // 如果不在引号内、不在转义状态且遇到空白字符，则停止
            if (!quoter.isQuoted() && !quoter.isEscaped() && isBlank(c)) {
                break;
            }
            // 如果是有效的码点，添加到值中
            if (quoter.isCodePoint()) {
                // 处理弱引号中的转义字符
                if (quoter.isEscaped() && quoter.isWeaklyQuoted() && c != '"') {
                    value.append('\\');
                }
                value.append(c);
            }
            index++;
        }
        // 创建文本令牌并添加到构建器中
        builder.add(new CliTokenImpl(true, s.substring(from, index), value.toString()));
        return index;
    }

    /**
     * 提取空白令牌
     * <p>
     * 从指定位置开始提取连续的空白字符（空格或制表符），
     * 将它们作为一个空白令牌添加到构建器中。
     * </p>
     *
     * @param s 原始字符串
     * @param index 开始位置
     * @param builder 用于存储生成的令牌的列表
     * @return 下一个要处理的索引位置
     */
    private static int blankToken(String s, int index, List<CliToken> builder) {
        // 记录空白字符的起始位置
        int from = index;
        // 跳过所有连续的空白字符
        while (index < s.length() && isBlank(s.charAt(index))) {
            index++;
        }
        // 创建空白令牌并添加到构建器中
        builder.add(new CliTokenImpl(false, s.substring(from, index)));
        return index;
    }

    /**
     * 判断字符是否为空白字符
     * <p>
     * 空白字符包括空格和制表符
     * </p>
     *
     * @param c 要判断的字符
     * @return true表示是空白字符，false表示不是
     */
    private static boolean isBlank(char c) {
        return c == ' ' || c == '\t';
    }
}
