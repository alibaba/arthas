package com.taobao.arthas.core.shell.impl;

import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandBuilder;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.command.internal.GrepHandler;
import com.taobao.arthas.core.shell.command.internal.PlainTextHandler;
import com.taobao.arthas.core.shell.command.internal.WordCountHandler;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.NoOpHandler;

import java.util.Arrays;
import java.util.List;

/**
 * 内置命令解析器
 * 负责解析和注册Arthas的内置命令
 *
 * 内置命令包括：
 * - exit/quit: 退出Arthas
 * - jobs: 查看作业列表
 * - fg: 将后台作业切换到前台
 * - bg: 将作业切换到后台
 * - kill: 终止作业
 * - grep: 文本搜索
 * - wc: 字数统计
 *
 * @author beiwei30 on 23/11/2016.
 */
class BuiltinCommandResolver implements CommandResolver {

    /**
     * 命令处理器
     * 使用无操作处理器作为默认处理器，实际的命令处理逻辑由其他机制处理
     */
    private Handler<CommandProcess> handler;

    /**
     * 构造函数
     * 创建内置命令解析器实例，并初始化一个无操作处理器
     */
    public BuiltinCommandResolver() {
        this.handler = new NoOpHandler<CommandProcess>();
    }

    /**
     * 返回所有内置命令的列表
     * 每个命令都使用命令构建器创建，并配置了对应的处理器
     *
     * @return 内置命令列表
     */
    @Override
    public List<Command> commands() {
        return Arrays.asList(
                // exit命令 - 退出Arthas
                CommandBuilder.command("exit").processHandler(handler).build(),
                // quit命令 - 退出Arthas（与exit功能相同）
                CommandBuilder.command("quit").processHandler(handler).build(),
                // jobs命令 - 列出所有后台作业
                CommandBuilder.command("jobs").processHandler(handler).build(),
                // fg命令 - 将后台作业切换到前台继续执行
                CommandBuilder.command("fg").processHandler(handler).build(),
                // bg命令 - 将作业切换到后台继续执行
                CommandBuilder.command("bg").processHandler(handler).build(),
                // kill命令 - 终止指定的作业
                CommandBuilder.command("kill").processHandler(handler).build(),
                // 纯文本处理器 - 处理普通文本命令
                CommandBuilder.command(PlainTextHandler.NAME).processHandler(handler).build(),
                // grep命令 - 文本搜索命令
                CommandBuilder.command(GrepHandler.NAME).processHandler(handler).build(),
                // wc命令 - 字数统计命令
                CommandBuilder.command(WordCountHandler.NAME).processHandler(handler).build()
        );
    }
}
