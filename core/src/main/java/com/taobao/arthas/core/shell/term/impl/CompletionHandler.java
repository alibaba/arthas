package com.taobao.arthas.core.shell.term.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;

import io.termd.core.function.Consumer;
import io.termd.core.readline.Completion;

import java.util.Collections;
import java.util.List;

/**
 * 命令行补全处理器
 *
 * 该类作为Consumer<Completion>的实现，负责处理命令行补全请求
 * 当用户触发补全时（如按Tab键），termd库会调用该处理器
 * 它将termd的Completion对象转换为Arthas内部的Completion对象，然后调用具体的补全处理逻辑
 *
 * @author beiwei30 on 23/11/2016.
 */
class CompletionHandler implements Consumer<Completion> {
    // 日志记录器，用于记录补全过程中的错误信息
    private static final Logger logger = LoggerFactory.getLogger(CompletionHandler.class);
    // Arthas内部的补全处理器，负责实际的补全逻辑
    private final Handler<com.taobao.arthas.core.shell.cli.Completion> completionHandler;
    // 当前的会话对象，包含会话相关的所有信息
    private final Session session;

    /**
     * 构造命令行补全处理器
     *
     * @param completionHandler Arthas内部的补全处理器，负责实际的补全逻辑
     * @param session 当前的会话对象
     */
    public CompletionHandler(Handler<com.taobao.arthas.core.shell.cli.Completion> completionHandler, Session session) {
        this.completionHandler = completionHandler;
        this.session = session;
    }

    /**
     * 处理命令行补全请求
     *
     * 该方法是Consumer接口的实现，当用户触发补全时被调用
     * 主要步骤：
     * 1. 将termd提供的Unicode码点数组转换为字符串
     * 2. 将命令行字符串分词为CliToken列表
     * 3. 创建CompletionAdaptor适配器对象
     * 4. 调用Arthas内部的补全处理器执行补全逻辑
     * 5. 捕获并记录所有异常，防止补全错误导致程序崩溃
     *
     * @param completion termd库提供的Completion对象，包含补全上下文信息
     */
    @Override
    public void accept(final Completion completion) {
        try {
            // 将termd提供的Unicode码点数组转换为字符串
            final String line = io.termd.core.util.Helper.fromCodePoints(completion.line());
            // 将命令行字符串分词为CliToken列表，并返回不可修改的列表
            final List<CliToken> tokens = Collections.unmodifiableList(CliTokens.tokenize(line));
            // 创建CompletionAdaptor适配器，将termd的Completion转换为Arthas的Completion接口
            com.taobao.arthas.core.shell.cli.Completion comp = new CompletionAdaptor(line, tokens, completion, session);
            // 调用Arthas内部的补全处理器执行实际的补全逻辑
            completionHandler.handle(comp);
        } catch (Throwable t) {
            // 捕获所有异常，防止补全错误导致程序崩溃
            // 打印堆栈跟踪已被注释，改为使用日志记录
            // t.printStackTrace();
            logger.error("completion error", t);
        }
    }
}
