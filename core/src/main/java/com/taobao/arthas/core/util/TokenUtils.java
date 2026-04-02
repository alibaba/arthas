package com.taobao.arthas.core.util;

import java.util.List;

import com.taobao.arthas.core.shell.cli.CliToken;

/**
 * 命令行Token（标记）工具类
 * <p>
 * 提供对命令行输入进行分词后的Token（标记）的查询和操作方法。
 * Token是命令行解析的基本单元，可以是文本、空格等类型。
 * </p>
 *
 * @author gehui 2017-07-27 11:39:56
 */
public class TokenUtils {

    /**
     * 查找第一个文本类型的Token
     * <p>
     * 遍历Token列表，返回第一个类型为文本的Token。
     * 文本Token通常是命令或参数的实际内容。
     * </p>
     *
     * @param tokens Token列表
     * @return 第一个文本Token，如果不存在则返回null
     */
    public static CliToken findFirstTextToken(List<CliToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        CliToken first = null;
        for (CliToken token : tokens) {
            if (token != null && token.isText()) {
                first = token;
                break;
            }
        }
        return first;
    }

    /**
     * 查找最后一个文本类型的Token
     * <p>
     * 从列表末尾向前遍历，返回最后一个类型为文本的Token。
     * 通常用于获取用户当前输入的最后一个完整参数。
     * </p>
     * 注：反向遍历是为了优化性能，因为最后一个token通常在列表末尾
     *
     * @param tokens Token列表
     * @return 最后一个文本Token，如果不存在则返回null
     */
    public static CliToken findLastTextToken(List<CliToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        // 从后往前遍历，避免遍历整个列表（issue #165）
        for (int i = tokens.size() - 1; i >= 0; i--) {
            CliToken token = tokens.get(i);
            if (token != null && token.isText()) {
                return token;
            }
        }
        return null;
    }

    /**
     * 查找第二个文本Token的内容
     * <p>
     * 跳过第一个文本Token，返回第二个文本Token的值。
     * 通常用于获取命令后的第一个参数。
     * </p>
     *
     * @param tokens Token列表
     * @return 第二个文本Token的值，如果不存在则返回null
     */
    public static String findSecondTokenText(List<CliToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        boolean first = true;
        for (CliToken token : tokens) {
            if (token != null && token.isText()) {
                if (first) {
                    first = false;
                } else {
                    return token.value();
                }
            }
        }
        return null;
    }

    /**
     * 获取列表中最后一个Token
     * <p>
     * 直接返回列表中的最后一个元素，不区分Token类型。
     * </p>
     *
     * @param tokens Token列表
     * @return 最后一个Token，如果列表为空则返回null
     */
    public static CliToken getLast(List<CliToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        } else {
            return tokens.get(tokens.size() -1);
        }
    }

    /**
     * 获取前一个参数的值
     * <p>
     * 用于命令行自动补全场景，当用户正在输入时，获取前一个完整的参数。
     * </p>
     *
     * @param tokens Token列表
     * @param lastToken 最后一个Token的内容（可能为空或不完整）
     * @return 前一个参数的值，如果没有则返回空字符串
     */
    public static String retrievePreviousArg(List<CliToken> tokens, String lastToken) {
        if (StringUtils.isBlank(lastToken) && tokens.size() > 2) {
            // 场景：tokens = { " ", "CLASS_NAME", " "}
            // 最后一个token为空格，前一个token是完整的类名
            return tokens.get(tokens.size() - 2).value();
        } else if (tokens.size() > 3) {
            // 场景：tokens = { " ", "CLASS_NAME", " ", "PARTIAL_METHOD_NAME"}
            // 最后一个token是部分输入的方法名，前一个完整的token是类名
            return tokens.get(tokens.size() - 3).value();
        } else {
            return Constants.EMPTY_STRING;
        }
    }
}
