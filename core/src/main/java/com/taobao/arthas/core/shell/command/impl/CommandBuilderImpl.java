package com.taobao.arthas.core.shell.command.impl;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandBuilder;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.middleware.cli.CLI;

import java.util.Collections;

/**
 * 命令构建器实现类
 *
 * 该类用于构建Arthas命令，通过构建者模式提供流畅的API来设置命令的各种属性
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CommandBuilderImpl extends CommandBuilder {

    // 命令名称
    private final String name;

    // CLI（命令行接口）对象，用于解析命令行参数
    private final CLI cli;

    // 命令处理器，用于处理命令的执行逻辑
    private Handler<CommandProcess> processHandler;

    // 自动补全处理器，用于处理命令的自动补全逻辑
    private Handler<Completion> completeHandler;

    /**
     * 构造函数
     *
     * @param name 命令名称
     * @param cli CLI对象，用于解析命令行参数
     */
    public CommandBuilderImpl(String name, CLI cli) {
        this.name = name;
        this.cli = cli;
    }

    /**
     * 设置命令处理器
     *
     * @param handler 命令处理器
     * @return 当前构建器对象，支持链式调用
     */
    @Override
    public CommandBuilderImpl processHandler(Handler<CommandProcess> handler) {
        processHandler = handler;
        return this;
    }

    /**
     * 设置自动补全处理器
     *
     * @param handler 自动补全处理器
     * @return 当前构建器对象，支持链式调用
     */
    @Override
    public CommandBuilderImpl completionHandler(Handler<Completion> handler) {
        completeHandler = handler;
        return this;
    }

    /**
     * 构建命令对象
     *
     * @return 构建好的命令对象
     */
    @Override
    public Command build() {
        return new CommandImpl();
    }

    /**
     * 命令实现类
     *
     * 该内部类实现了Command接口，封装了命令的执行和补全逻辑
     */
    private class CommandImpl extends Command {

        /**
         * 获取命令名称
         *
         * @return 命令名称
         */
        @Override
        public String name() {
            return name;
        }

        /**
         * 获取CLI对象
         *
         * @return CLI对象
         */
        @Override
        public CLI cli() {
            return cli;
        }

        /**
         * 获取命令处理器
         *
         * @return 命令处理器
         */
        @Override
        public Handler<CommandProcess> processHandler() {
            return processHandler;
        }

        /**
         * 执行自动补全
         *
         * 该方法处理命令的自动补全逻辑。如果设置了自定义的补全处理器，则使用该处理器；
         * 否则使用父类的默认补全逻辑。如果在补全过程中发生异常，则返回空列表。
         *
         * @param completion 补全对象
         */
        @Override
        public void complete(final Completion completion) {
            // 如果设置了自定义的补全处理器
            if (completeHandler != null) {
                try {
                    // 调用自定义补全处理器处理补全请求
                    completeHandler.handle(completion);
                } catch (Throwable t) {
                    // 如果补全过程中发生异常，返回空的候选列表
                    completion.complete(Collections.<String>emptyList());
                }
            } else {
                // 如果没有设置自定义补全处理器，使用父类的默认补全逻辑
                super.complete(completion);
            }
        }
    }
}
