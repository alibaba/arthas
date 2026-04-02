package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliToken;
import io.termd.core.function.Function;

import java.util.LinkedList;
import java.util.List;

/**
 * 标准输出处理器抽象类
 * <p>
 * 该类是所有标准输出处理器的基类，用于处理命令行输出的各种操作，
 * 如管道过滤、输出重定向等。实现了Function接口，可以对输入字符串进行处理。
 * </p>
 *
 * @author beiwei30 on 20/12/2016.
 */
public abstract class StdoutHandler implements Function<String, String> {

    /**
     * 根据命令行token注入相应的标准输出处理器
     * <p>
     * 该方法分析命令行token列表，根据第一个文本token的类型创建相应的处理器实例。
     * 支持的处理器包括：
     * <ul>
     * <li>GrepHandler - 用于grep过滤</li>
     * <li>PlainTextHandler - 用于纯文本输出</li>
     * <li>WordCountHandler - 用于统计行数/字数</li>
     * <li>TeeHandler - 用于输出重定向到文件</li>
     * </ul>
     * </p>
     *
     * @param tokens 命令行token列表
     * @return 对应的StdoutHandler实例，如果无法识别则返回null
     */
    public static StdoutHandler inject(List<CliToken> tokens) {
        // 查找第一个文本类型的token
        CliToken firstTextToken = null;
        for (CliToken token : tokens) {
            if (token.isText()) {
                firstTextToken = token;
                break;
            }
        }

        // 如果没有找到文本token，返回null
        if (firstTextToken == null) {
            return null;
        }

        // 根据第一个文本token的值创建相应的处理器
        if (firstTextToken.value().equals(GrepHandler.NAME)) {
            return GrepHandler.inject(tokens);
        } else if (firstTextToken.value().equals(PlainTextHandler.NAME)) {
            return PlainTextHandler.inject(tokens);
        } else if (firstTextToken.value().equals(WordCountHandler.NAME)) {
            return WordCountHandler.inject(tokens);
        } else if (firstTextToken.value().equals(TeeHandler.NAME)){
            return TeeHandler.inject(tokens);
        } else{
            return null;
        }
    }

    /**
     * 从命令行token列表中解析指定命令的参数
     * <p>
     * 该方法遍历token列表，找到指定命令后的所有文本token作为参数返回。
     * 例如：对于token列表 ["grep", "abc", "def"]，指定command为"grep"，
     * 则返回 ["abc", "def"]
     * </p>
     *
     * @param tokens 命令行token列表
     * @param command 要查找的命令名称
     * @return 该命令的参数列表
     */
    public static List<String> parseArgs(List<CliToken> tokens, String command) {
        List<String> args = new LinkedList<String>();
        boolean found = false;
        // 遍历所有token
        for (CliToken token : tokens) {
            // 找到指定的命令
            if (token.isText() && token.value().equals(command)) {
                found = true;
            } else if (token.isText() && found) {
                // 收集命令后的所有文本token作为参数
                args.add(token.value());
            }
        }
        return args;
    }

    /**
     * 处理输入字符串
     * <p>
     * 这是Function接口的实现方法，默认行为是原样返回输入字符串。
     * 子类可以重写此方法以实现特定的处理逻辑，如过滤、转换、统计等。
     * </p>
     *
     * @param s 输入字符串
     * @return 处理后的字符串，默认返回原字符串
     */
    @Override
    public String apply(String s) {
        return s;
    }
}
