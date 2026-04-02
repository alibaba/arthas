package com.taobao.arthas.core.shell.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令注册表，包含Shell已知的所有命令<p/>
 * <p>
 * 这是一个可变的命令解析器，支持动态注册和注销命令
 * 使用ConcurrentHashMap来存储命令，保证线程安全
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CommandRegistry implements CommandResolver {
    /**
     * 命令映射表，使用ConcurrentHashMap保证线程安全
     * key: 命令名称
     * value: Command对象
     */
    final ConcurrentHashMap<String, Command> commandMap = new ConcurrentHashMap<String, Command>();

    /**
     * 创建一个新的命令注册表
     *
     * @return 新创建的命令注册表实例
     */
    public static CommandRegistry create() {
        return new CommandRegistry();
    }

    /**
     * 注册单个命令（通过命令类）
     *
     * @param command 要注册的命令类，必须是AnnotatedCommand的子类
     * @return 返回当前注册表实例，支持链式调用
     */
    public CommandRegistry registerCommand(Class<? extends AnnotatedCommand> command) {
        return registerCommand(Command.create(command));
    }

    /**
     * 注册单个命令（通过Command对象）
     *
     * @param command 要注册的命令对象
     * @return 返回当前注册表实例，支持链式调用
     */
    public CommandRegistry registerCommand(Command command) {
        return registerCommands(Collections.singletonList(command));
    }

    /**
     * 批量注册多个命令
     *
     * @param commands 要注册的命令列表
     * @return 返回当前注册表实例，支持链式调用
     */
    public CommandRegistry registerCommands(List<Command> commands) {
        // 遍历命令列表，将每个命令注册到映射表中
        for (Command command : commands) {
            commandMap.put(command.name(), command);
        }
        return this;
    }


    /**
     * 注销（移除）一个命令
     *
     * @param commandName 要注销的命令名称
     * @return 返回当前注册表实例，支持链式调用
     */
    public CommandRegistry unregisterCommand(String commandName) {
        commandMap.remove(commandName);
        return this;
    }

    /**
     * 获取所有已注册的命令列表
     * 实现CommandResolver接口的方法
     *
     * @return 包含所有已注册命令的列表，返回的是一个新的ArrayList，避免外部修改内部数据
     */
    @Override
    public List<Command> commands() {
        return new ArrayList<Command>(commandMap.values());
    }
}
