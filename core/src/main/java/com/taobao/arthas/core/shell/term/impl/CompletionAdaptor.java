package com.taobao.arthas.core.shell.term.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * 命令行补全适配器
 *
 * 该类是Arthas内部的Completion接口与termd库的Completion接口之间的适配器
 * 负责将Arthas的补全请求转换为termd库能够处理的格式
 *
 * @author beiwei30 on 23/11/2016.
 */
class CompletionAdaptor implements Completion {
    // 当前的会话对象，包含会话相关的所有信息
    private final Session session;
    // 用户输入的原始命令行文本
    private final String line;
    // 命令行分词后的token列表
    private final List<CliToken> tokens;
    // termd库的Completion接口实例，用于实际的补全操作
    private final io.termd.core.readline.Completion completion;

    /**
     * 构造命令行补全适配器
     *
     * @param line 用户输入的原始命令行文本
     * @param tokens 命令行分词后的token列表
     * @param completion termd库的Completion接口实例
     * @param session 当前的会话对象
     */
    public CompletionAdaptor(String line, List<CliToken> tokens, io.termd.core.readline.Completion completion,
                             Session session) {
        this.line = line;
        this.tokens = tokens;
        this.completion = completion;
        this.session = session;
    }

    /**
     * 获取当前会话对象
     *
     * @return 当前的会话对象，包含会话相关的所有信息
     */
    @Override
    public Session session() {
        return session;
    }

    /**
     * 获取用户输入的原始命令行文本
     *
     * @return 原始命令行文本字符串
     */
    @Override
    public String rawLine() {
        return line;
    }

    /**
     * 获取命令行分词后的token列表
     *
     * @return 命令行token列表，包含命令、选项、参数等信息
     */
    @Override
    public List<CliToken> lineTokens() {
        return tokens;
    }

    /**
     * 执行命令行补全操作
     *
     * 该方法根据候选补全列表，自动选择最合适的补全策略：
     * 1. 如果有多个候选项，先尝试补全最长公共前缀
     * 2. 如果最长公共前缀不比当前token更长，则显示所有候选项
     * 3. 如果只有一个候选项，直接补全
     * 4. 如果没有候选项，结束补全
     *
     * @param candidates 候选补全字符串列表
     */
    @Override
    public void complete(List<String> candidates) {
        // 获取最后一个token的值，如果tokens为空或最后一个token为空白，则使用空字符串
        String lastToken = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1).value();
        if(StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }
        // 如果有多个候选项，先尝试补全最长公共前缀
        if (candidates.size() > 1) {
            // 查找所有候选项的最长公共前缀
            String commonPrefix = CompletionUtils.findLongestCommonPrefix(candidates);
            if (commonPrefix.length() > 0) {
                if (!commonPrefix.equals(lastToken)) {
                    // 只有当公共前缀比最后一个token更长时才进行补全
                    if (commonPrefix.length() > lastToken.length()) {
                        // 计算需要补全的字符串部分（公共前缀减去已输入的token）
                        String strToComplete = commonPrefix.substring(lastToken.length());
                        // 执行补全操作，false表示不终止补全
                        completion.complete(io.termd.core.util.Helper.toCodePoints(strToComplete), false);
                        return;
                    }
                }
            }
        }
        // 如果有候选项但没有补全公共前缀，或者只有一个候选项，则显示所有候选项作为建议
        if (candidates.size() > 0) {
            // 将所有候选项字符串转换为Unicode码点数组列表
            List<int[]> suggestions = new LinkedList<int[]>();
            for (String candidate : candidates) {
                suggestions.add(io.termd.core.util.Helper.toCodePoints(candidate));
            }
            // 显示补全建议列表
            completion.suggest(suggestions);
        } else {
            // 没有候选项，结束补全操作
            completion.end();
        }
    }

    /**
     * 直接完成命令行输入
     *
     * 该方法用于直接将指定的补全值插入到命令行中
     *
     * @param value 要补全的字符串值
     * @param terminal 是否终止补全操作，true表示补全后不再显示建议列表
     */
    @Override
    public void complete(String value, boolean terminal) {
        // 将字符串转换为Unicode码点数组，然后调用termd的complete方法
        completion.complete(io.termd.core.util.Helper.toCodePoints(value), terminal);
    }
}
