package com.taobao.arthas.core.shell.system.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.session.Session;

import java.util.List;

/**
 * 命令补全实现类
 * 该类是对Completion接口的包装实现，用于处理命令行的自动补全功能
 *
 * 主要功能：
 * - 包装原始的Completion对象，提供命令补全功能
 * - 保存当前的命令行内容和解析后的令牌列表
 * - 委托实际的补全操作给原始的Completion对象
 *
 * @author beiwei30 on 23/11/2016.
 */
class CommandCompletion implements Completion {

    /**
     * 原始的Completion对象
     * 实际的补全操作会委托给这个对象执行
     */
    private final Completion completion;

    /**
     * 命令行的原始内容
     * 用户输入的完整命令行文本
     */
    private final String line;

    /**
     * 解析后的命令行令牌列表
     * 将原始命令行文本解析为多个令牌，便于后续处理
     */
    private final List<CliToken> newTokens;

    /**
     * 构造函数
     *
     * @param completion 原始的Completion对象，用于委托实际的补全操作
     * @param line 命令行的原始内容
     * @param newTokens 解析后的命令行令牌列表
     */
    public CommandCompletion(Completion completion, String line, List<CliToken> newTokens) {
        this.completion = completion;
        this.line = line;
        this.newTokens = newTokens;
    }

    /**
     * 获取当前的会话对象
     * 委托给原始的Completion对象获取
     *
     * @return 当前的会话对象
     */
    @Override
    public Session session() {
        return completion.session();
    }

    /**
     * 获取命令行的原始内容
     *
     * @return 用户输入的完整命令行文本
     */
    @Override
    public String rawLine() {
        return line;
    }

    /**
     * 获取解析后的命令行令牌列表
     *
     * @return 解析后的令牌列表
     */
    @Override
    public List<CliToken> lineTokens() {
        return newTokens;
    }

    /**
     * 完成命令补全
     * 提供多个候选补全结果供用户选择
     * 委托给原始的Completion对象执行
     *
     * @param candidates 候选补全结果列表
     */
    @Override
    public void complete(List<String> candidates) {
        completion.complete(candidates);
    }

    /**
     * 完成命令补全
     * 提供单个补全结果，并指定是否是最终结果
     * 委托给原始的Completion对象执行
     *
     * @param value 补全的值
     * @param terminal 是否是最终结果（true表示补全完成，false表示可能还有后续补全）
     */
    @Override
    public void complete(String value, boolean terminal) {
        completion.complete(value, terminal);
    }
}
