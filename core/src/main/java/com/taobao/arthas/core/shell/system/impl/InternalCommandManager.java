package com.taobao.arthas.core.shell.system.impl;

import com.taobao.arthas.core.command.BuiltinCommandPack;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * 内部命令管理器
 *
 * 负责管理和解析Arthas的内部命令，提供命令查找和自动补全功能
 * 支持多个命令解析器，可以动态扩展命令来源
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class InternalCommandManager {

    /**
     * 命令解析器列表
     *
     * 支持多个命令解析器，每个解析器可以提供一组命令
     */
    private final List<CommandResolver> resolvers;

    /**
     * 构造函数，使用可变参数初始化命令解析器列表
     *
     * @param resolvers 命令解析器数组
     */
    public InternalCommandManager(CommandResolver... resolvers) {
        this.resolvers = Arrays.asList(resolvers);
    }

    /**
     * 构造函数，使用列表初始化命令解析器列表
     *
     * @param resolvers 命令解析器列表
     */
    public InternalCommandManager(List<CommandResolver> resolvers) {
        this.resolvers = resolvers;
    }

    /**
     * 获取所有命令解析器
     *
     * @return 命令解析器列表
     */
    public List<CommandResolver> getResolvers() {
        return resolvers;
    }

    /**
     * 根据命令名称获取命令对象
     *
     * 遍历所有命令解析器，查找匹配的命令
     * 内建命令会在ShellLineHandler中提前处理，所以这里只查找内建命令包中的命令
     *
     * @param commandName 命令名称
     * @return 命令对象，如果未找到则返回null
     */
    public Command getCommand(String commandName) {
        Command command = null;
        // 遍历所有命令解析器
        for (CommandResolver resolver : resolvers) {
            // 内建命令在ShellLineHandler里提前处理了，所以这里不需要再查找内建命令
            if (resolver instanceof BuiltinCommandPack) {
                command = getCommand(resolver, commandName);
                if (command != null) {
                    break;
                }
            }
        }
        return command;
    }

    /**
     * 执行命令自动补全
     *
     * 根据用户输入的命令行tokens，提供命令名称或命令参数的自动补全建议
     * 支持管道命令的补全，会查找最后一个管道符后的命令进行补全
     *
     * @param completion 补全对象，用于通知补全进度和结果
     */
    public void complete(final Completion completion) {
        // 获取命令行的所有tokens
        List<CliToken> lineTokens = completion.lineTokens();
        // 查找最后一个管道符的位置
        int index = findLastPipe(lineTokens);
        // 提取管道符之后的所有tokens（如果没有管道符，则是全部tokens）
        LinkedList<CliToken> tokens = new LinkedList<CliToken>(lineTokens.subList(index + 1, lineTokens.size()));

        // 移除开头的空白字符
        while (tokens.size() > 0 && tokens.getFirst().isBlank()) {
            tokens.removeFirst();
        }

        // tokens数量 > 1 表示已经输入了命令文本并且后面还有其他内容
        // 说明是在补全命令的参数
        if (tokens.size() > 1) {
            completeSingleCommand(completion, tokens);
        } else {
            // tokens数量 <= 1 表示正在补全命令名称
            completeCommands(completion, tokens);
        }
    }

    /**
     * 补全命令名称
     *
     * 根据用户输入的前缀，查找所有匹配的命令名称
     * 如果只有一个匹配项，直接补全完整命令名
     * 如果有多个匹配项，尝试补全最长公共前缀
     * 如果没有公共前缀，显示所有匹配的命令名列表
     *
     * @param completion 补全对象
     * @param tokens 命令行tokens列表
     */
    private void completeCommands(Completion completion, LinkedList<CliToken> tokens) {
        // 获取用户输入的命令前缀（可能为空）
        String prefix = tokens.size() > 0 ? tokens.getFirst().value() : "";
        List<String> names = new LinkedList<String>();

        // 遍历所有命令解析器，收集匹配的命令名称
        for (CommandResolver resolver : resolvers) {
            for (Command command : resolver.commands()) {
                String name = command.name();
                // 检查命令是否为隐藏命令
                boolean hidden = command.cli() != null && command.cli().isHidden();
                // 如果命令名以prefix开头，不在已收集列表中，且不是隐藏命令，则添加到列表
                if (name.startsWith(prefix) && !names.contains(name) && !hidden) {
                    names.add(name);
                }
            }
        }

        // 如果只有一个匹配项，直接补全完整命令名
        if (names.size() == 1) {
            completion.complete(names.get(0).substring(prefix.length()), true);
        } else {
            // 如果有多个匹配项，尝试找最长公共前缀
            String commonPrefix = CompletionUtils.findLongestCommonPrefix(names);
            // 如果公共前缀比用户输入的前缀长，则补全公共前缀部分
            if (commonPrefix.length() > prefix.length()) {
                completion.complete(commonPrefix.substring(prefix.length()), false);
            } else {
                // 没有公共前缀可以补全，显示所有匹配的命令名列表
                completion.complete(names);
            }
        }
    }

    /**
     * 补全单个命令的参数
     *
     * 当用户已经输入了命令名称后，补全命令的参数
     * 会找到对应的命令对象，并调用该命令的补全逻辑
     *
     * @param completion 补全对象
     * @param tokens 命令行tokens列表
     */
    private void completeSingleCommand(Completion completion, LinkedList<CliToken> tokens) {
        // 使用列表迭代器遍历tokens
        ListIterator<CliToken> it = tokens.listIterator();
        while (it.hasNext()) {
            CliToken ct = it.next();
            it.remove();
            // 找到第一个文本类型的token（即命令名称）
            if (ct.isText()) {
                // 收集剩余的所有tokens作为命令参数
                final List<CliToken> newTokens = new ArrayList<CliToken>();
                while (it.hasNext()) {
                    newTokens.add(it.next());
                }
                // 将参数tokens拼接成字符串
                StringBuilder tmp = new StringBuilder();
                for (CliToken token : newTokens) {
                    tmp.append(token.raw());
                }
                final String line = tmp.toString();

                // 在所有命令解析器中查找该命令
                for (CommandResolver resolver : resolvers) {
                    Command command = getCommand(resolver, ct.value());
                    if (command != null) {
                        // 找到命令后，调用该命令的补全方法
                        command.complete(new CommandCompletion(completion, line, newTokens));
                        return;
                    }
                }
                // 如果没有找到命令，返回空列表
                completion.complete(Collections.<String>emptyList());
            }
        }
    }

    /**
     * 从命令解析器中获取指定名称的命令
     *
     * 遍历命令解析器提供的所有命令，查找名称匹配的命令
     *
     * @param commandResolver 命令解析器
     * @param name 命令名称
     * @return 命令对象，如果未找到则返回null
     */
    private static Command getCommand(CommandResolver commandResolver, String name) {
        // 获取命令解析器提供的所有命令
        List<Command> commands = commandResolver.commands();
        if (commands == null || commands.isEmpty()) {
            return null;
        }

        // 遍历命令列表，查找名称匹配的命令
        for (Command command : commands) {
            if (name.equals(command.name())) {
                return command;
            }
        }
        return null;
    }

    /**
     * 查找命令行中最后一个管道符的位置
     *
     * 用于处理管道命令，需要找到最后一个管道符之后的部分进行补全
     *
     * @param lineTokens 命令行tokens列表
     * @return 最后一个管道符的索引位置，如果没有管道符则返回-1
     */
    private static int findLastPipe(List<CliToken> lineTokens) {
        int index = -1;
        // 遍历所有tokens，查找管道符
        for (int i = 0; i < lineTokens.size(); i++) {
            if ("|".equals(lineTokens.get(i).value())) {
                index = i;
            }
        }
        return index;
    }
}
