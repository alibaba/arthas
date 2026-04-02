package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;

/**
 * 命令管理器自动补全处理器
 *
 * 该类实现了Handler接口，用于处理命令行的自动补全请求。
 * 当用户在终端输入命令并触发自动补全（通常是按Tab键）时，
 * 该处理器负责将补全请求转发给InternalCommandManager进行处理。
 *
 * InternalCommandManager会根据当前输入的内容，分析上下文，
 * 并提供可能的命令、选项或参数的补全建议。
 *
 * 这是Arthas命令行交互体验的重要组成部分，提升了用户的使用效率。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class CommandManagerCompletionHandler implements Handler<Completion> {

    /**
     * 内部命令管理器
     * 该属性持有对InternalCommandManager的引用，用于处理命令的自动补全逻辑
     */
    private InternalCommandManager commandManager;

    /**
     * 构造函数
     *
     * @param commandManager 内部命令管理器实例，不能为null
     *                       该参数会被保存到成员变量中，供handle方法使用
     */
    public CommandManagerCompletionHandler(InternalCommandManager commandManager) {
        this.commandManager = commandManager;
    }

    /**
     * 处理自动补全请求
     *
     * 该方法是Handler接口的实现，当用户触发自动补全时会调用该方法。
     * 方法会将Completion对象转发给InternalCommandManager的complete方法。
     *
     * Completion对象包含了以下关键信息：
     * - 当前行缓冲区内容（line buffer）
     * - 光标位置（cursor position）
     * - 补全候选列表（candidates list）
     *
     * InternalCommandManager会根据这些信息：
     * 1. 解析当前输入的命令
     * 2. 分析光标所在位置的上下文
     * 3. 根据命令定义和可用选项生成补全建议
     * 4. 将补全候选填充到Completion对象中
     *
     * @param completion 补全请求对象，包含了当前输入和补全候选的信息
     */
    @Override
    public void handle(Completion completion) {
        commandManager.complete(completion);
    }
}
