package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.middleware.cli.CLI;

import java.util.Collections;
import java.util.List;

/**
 * 命令抽象类
 *
 * 定义了命令的基本接口和行为规范
 * 提供了从注解类创建命令的工厂方法
 */
public abstract class Command {

    /**
     * 从使用CLI注解的Java类创建命令对象
     *
     * 这是一个工厂方法，用于将带有注解的命令类包装成Command对象
     * 通过反射机制读取类上的注解信息，自动构建命令的元数据
     *
     * @param clazz 带注解的命令类，必须继承自AnnotatedCommand
     * @return 创建的命令对象
     */
    public static Command create(final Class<? extends AnnotatedCommand> clazz) {
        // 使用AnnotatedCommandImpl实现类来包装带注解的命令类
        return new AnnotatedCommandImpl(clazz);
    }

    /**
     * 获取命令名称
     *
     * 子类可以重写此方法返回自定义的命令名称
     *
     * @return 命令名称，默认返回null
     */
    public String name() {
        return null;
    }

    /**
     * 获取命令行接口(CLI)对象
     *
     * CLI对象封装了命令的参数定义、选项定义和帮助信息等
     * 子类可以重写此方法返回自定义的CLI对象
     *
     * @return 命令行接口对象，默认返回null
     */
    public CLI cli() {
        return null;
    }

    /**
     * 创建命令处理器
     *
     * 返回一个处理器对象，该对象会在命令被执行时被调用
     * 处理器接收CommandProcess参数，通过它可以访问命令参数、会话信息等
     *
     * @return 命令处理器，用于处理命令的执行逻辑
     */
    public abstract Handler<CommandProcess> processHandler();

    /**
     * 执行命令自动补全
     *
     * 当用户按下Tab键请求补全时，此方法会被调用
     * 默认实现返回空的补全列表，子类可以重写此方法提供自定义的补全逻辑
     * 补全完成后必须调用 {@link Completion#complete(List)} 或 {@link Completion#complete(String, boolean)} 方法来通知补全完成
     *
     * @param completion 补全对象，用于设置补全结果和通知补全完成
     */
    public void complete(Completion completion) {
        // 默认返回空的补全列表
        completion.complete(Collections.<String>emptyList());
    }
}
