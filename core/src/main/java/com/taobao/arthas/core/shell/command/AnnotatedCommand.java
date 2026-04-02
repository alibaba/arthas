package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.middleware.cli.CLI;

import java.util.List;

/**
 * 带注解的Java命令的基类
 *
 * 所有使用注解方式定义的Java命令都应该继承此类
 * 提供了命令名称获取、CLI对象获取、命令处理和自动补全等基本功能
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class AnnotatedCommand {

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
     * CLI对象用于解析命令行参数和选项，包括命令的帮助信息、参数定义等
     * 子类可以重写此方法返回自定义的CLI对象
     *
     * @return 命令行接口对象，默认返回null
     */
    public CLI cli() {
        return null;
    }

    /**
     * 处理命令执行的核心方法
     *
     * 当命令被调用时，此方法会被执行
     * 命令处理完成后必须调用 {@link CommandProcess#end()} 方法来结束命令处理
     *
     * @param process 命令处理对象，提供了与命令执行过程交互的能力
     */
    public abstract void process(CommandProcess process);

    /**
     * 执行命令自动补全
     *
     * 当用户按下Tab键请求补全时，此方法会被调用
     * 默认实现使用反射机制扫描类上的CLI注解，自动生成补全建议
     * 补全完成后必须调用 {@link Completion#complete(List)} 或 {@link Completion#complete(String, boolean)} 方法来通知补全完成
     *
     * @param completion 补全对象，用于设置补全结果和通知补全完成
     */
    public void complete(Completion completion) {
        // 使用CompletionUtils工具类，通过反射扫描当前类的CLI注解，自动生成补全建议
        CompletionUtils.complete(completion, this.getClass());
    }

}

