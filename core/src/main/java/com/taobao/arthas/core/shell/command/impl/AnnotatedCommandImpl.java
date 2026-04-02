package com.taobao.arthas.core.shell.command.impl;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

import java.util.Collections;

/**
 * 注解命令实现类
 *
 * 该类是AnnotatedCommand的默认实现，用于处理带有注解的命令类。
 * 它通过反射机制动态创建命令实例，并调用命令的处理方法。
 *
 * @author beiwei30 on 10/11/2016.
 */
public class AnnotatedCommandImpl extends Command {

    /**
     * CLI命令行接口对象，用于解析命令行参数和选项
     */
    private CLI cli;

    /**
     * 注解命令类的Class对象
     */
    private Class<? extends AnnotatedCommand> clazz;

    /**
     * 命令处理器，用于处理命令执行过程
     */
    private Handler<CommandProcess> processHandler = new ProcessHandler();

    /**
     * 构造函数
     *
     * @param clazz 注解命令类的Class对象
     */
    public AnnotatedCommandImpl(Class<? extends AnnotatedCommand> clazz) {
        this.clazz = clazz;
        // 使用CLIConfigurator定义命令行接口
        cli = CLIConfigurator.define(clazz, true);
        // 添加帮助选项
        cli.addOption(new Option().setArgName("help").setFlag(true).setShortName("h").setLongName("help")
                .setDescription("this help").setHelp(true));
    }

    /**
     * 判断命令类是否覆盖了name()方法
     *
     * 通过反射检查类是否声明了name()方法，以确定是否使用自定义的命令名称
     *
     * @param clazz 注解命令类的Class对象
     * @return 如果类覆盖了name()方法返回true，否则返回false
     */
    private boolean shouldOverridesName(Class<? extends AnnotatedCommand> clazz) {
        try {
            clazz.getDeclaredMethod("name");
            return true;
        } catch (NoSuchMethodException ignore) {
            return false;
        }
    }

    /**
     * 判断命令类是否覆盖了cli()方法
     *
     * 通过反射检查类是否声明了cli()方法，以确定是否使用自定义的CLI配置
     *
     * @param clazz 注解命令类的Class对象
     * @return 如果类覆盖了cli()方法返回true，否则返回false
     */
    private boolean shouldOverrideCli(Class<? extends AnnotatedCommand> clazz) {
        try {
            clazz.getDeclaredMethod("cli");
            return true;
        } catch (NoSuchMethodException ignore) {
            return false;
        }
    }

    /**
     * 获取命令名称
     *
     * 如果命令类覆盖了name()方法，则使用自定义的名称；否则使用CLI配置的名称
     *
     * @return 命令名称
     */
    @Override
    public String name() {
        // 检查是否使用了自定义的name()方法
        if (shouldOverridesName(clazz)) {
            try {
                return clazz.newInstance().name();
            } catch (Exception ignore) {
                // 如果实例化失败，使用cli.getName()作为默认值
            }
        }
        return cli.getName();
    }

    /**
     * 获取CLI命令行接口对象
     *
     * 如果命令类覆盖了cli()方法，则使用自定义的CLI配置；否则使用默认的CLI配置
     *
     * @return CLI命令行接口对象
     */
    @Override
    public CLI cli() {
        // 检查是否使用了自定义的cli()方法
        if (shouldOverrideCli(clazz)) {
            try {
                return clazz.newInstance().cli();
            } catch (Exception ignore) {
                // 如果实例化失败，使用默认的cli对象
            }
        }
        return cli;
    }

    /**
     * 处理命令执行
     *
     * 创建命令实例，注入命令行参数，并执行命令的处理逻辑。
     * 同时记录命令使用统计信息。
     *
     * @param process 命令处理上下文对象
     */
    private void process(CommandProcess process) {
        AnnotatedCommand instance;
        try {
            // 创建命令实例
            instance = clazz.newInstance();
        } catch (Exception e) {
            // 如果实例化失败，结束处理流程
            process.end();
            return;
        }
        // 将命令行参数注入到命令实例中
        CLIConfigurator.inject(process.commandLine(), instance);
        // 执行命令的处理逻辑
        instance.process(process);
        // 从会话中获取用户ID，用于统计报告
        String userId = process.session() != null ? process.session().getUserId() : null;
        // 记录Arthas使用成功统计
        UserStatUtil.arthasUsageSuccess(name(), process.args(), userId);
    }

    /**
     * 获取命令处理器
     *
     * 返回用于处理命令执行过程的处理器
     *
     * @return 命令处理器
     */
    @Override
    public Handler<CommandProcess> processHandler() {
        return processHandler;
    }

    /**
     * 处理命令自动补全
     *
     * 创建命令实例并调用其自动补全逻辑。如果出现异常，则调用父类的默认补全或返回空列表。
     *
     * @param completion 补全上下文对象
     */
    @Override
    public void complete(final Completion completion) {
        final AnnotatedCommand instance;
        try {
            // 创建命令实例
            instance = clazz.newInstance();
        } catch (Exception e) {
            // 如果实例化失败，使用父类的默认补全逻辑
            super.complete(completion);
            return;
        }

        try {
            // 调用命令实例的自动补全方法
            instance.complete(completion);
        } catch (Throwable t) {
            // 如果补全过程中出现异常，返回空的补全列表
            completion.complete(Collections.<String>emptyList());
        }
    }

    /**
     * 命令处理器内部类
     *
     * 实现Handler接口，用于处理命令执行过程。
     * 这是一个内部类，封装了process()方法的调用逻辑。
     */
    private class ProcessHandler implements Handler<CommandProcess> {
        /**
         * 处理命令执行
         *
         * 调用外部类的process()方法执行命令处理逻辑
         *
         * @param process 命令处理上下文对象
         */
        @Override
        public void handle(CommandProcess process) {
            process(process);
        }
    }

}
