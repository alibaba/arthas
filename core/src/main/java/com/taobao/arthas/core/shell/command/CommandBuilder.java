package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.impl.CommandBuilderImpl;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.middleware.cli.CLI;

/**
 * 命令构建器
 *
 * 用于构建命令对象的构建器类，支持链式调用
 * 提供了设置命令名称、CLI描述符、处理器等功能
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class CommandBuilder {

    /**
     * 创建一个新的命令构建器
     *
     * 使用命令名称创建构建器，命令需要自己通过 {@link CommandProcess#args()} 来管理选项和参数
     * 这种方式适用于简单的命令，不需要复杂的参数解析
     *
     * @param name 命令名称，用于标识和调用命令
     * @return 创建的命令构建器对象
     */
    public static CommandBuilder command(String name) {
        // 创建CommandBuilderImpl实例，不指定CLI对象
        return new CommandBuilderImpl(name, null);
    }

    /**
     * 使用CLI描述符创建一个新的命令构建器
     *
     * CLI描述符定义了命令的参数和选项结构
     * 命令执行时可以通过 {@link CommandProcess#commandLine()} 获取解析后的命令行对象
     * 这样可以方便地获取命令的参数和选项值
     *
     * @param cli CLI对象，封装了命令的参数定义、选项定义和帮助信息等
     * @return 创建的命令构建器对象
     */
    public static CommandBuilder command(CLI cli) {
        // 创建CommandBuilderImpl实例，指定CLI对象
        return new CommandBuilderImpl(cli.getName(), cli);
    }

    /**
     * 设置命令的处理器
     *
     * 处理器是命令执行时的核心逻辑，当命令被执行时会被调用
     * 处理器接收CommandProcess参数，通过它可以访问命令参数、会话信息、输出等
     *
     * @param handler 命令处理器，处理命令的具体执行逻辑
     * @return 当前命令构建器对象，支持链式调用
     */
    public abstract CommandBuilder processHandler(Handler<CommandProcess> handler);

    /**
     * 设置命令的自动补全处理器
     *
     * 当用户请求上下文相关的命令行补全时（通常按下Tab键），补全处理器会被调用
     * 补全处理器可以根据当前上下文提供合适的补全建议
     *
     * @param handler 补全处理器，处理命令的自动补全逻辑
     * @return 当前命令构建器对象，支持链式调用
     */
    public abstract CommandBuilder completionHandler(Handler<Completion> handler);

    /**
     * 构建命令对象
     *
     * 完成命令的配置后，调用此方法生成最终的Command对象
     * 返回的Command对象可以被注册到Shell中使用
     *
     * @return 构建完成的命令对象
     */
    public abstract Command build();

}
