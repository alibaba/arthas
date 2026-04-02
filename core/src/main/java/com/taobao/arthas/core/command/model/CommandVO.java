package com.taobao.arthas.core.command.model;

import com.taobao.middleware.cli.CLI;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令视图对象（Value Object）
 * <p>
 * 用于表示Arthas命令的完整信息，包括命令名称、描述、用法、摘要、选项列表和参数列表。
 * 主要用于在命令帮助信息中展示命令的完整用法和说明。
 * </p>
 *
 * @author gongdewei 2020/4/3
 */
public class CommandVO {
    // TODO: 移除cli字段，该字段标记为transient表示不进行序列化
    /**
     * CLI命令行接口对象
     * transient字段表示在序列化时忽略该字段
     */
    private transient CLI cli;

    /**
     * 命令名称
     * 例如：watch、trace、sc等
     */
    private String name;

    /**
     * 命令描述
     * 用于简要说明该命令的作用和功能
     */
    private String description;

    /**
     * 命令用法说明
     * 提供命令的使用示例和格式说明
     */
    private String usage;

    /**
     * 命令摘要
     * 提供命令功能的简要概述
     */
    private String summary;

    /**
     * 命令选项列表
     * 存储该命令支持的所有选项（如 --help, -n 等）
     */
    private List<CommandOptionVO> options = new ArrayList<CommandOptionVO>();

    /**
     * 命令参数列表
     * 存储该命令支持的所有参数（如类名、方法名等）
     */
    private List<ArgumentVO> arguments = new ArrayList<ArgumentVO>();

    /**
     * 默认构造函数
     * 创建一个空的命令视图对象
     */
    public CommandVO() {
    }

    /**
     * 构造函数
     * 创建一个带有名称和描述的命令视图对象
     *
     * @param name        命令名称
     * @param description 命令描述
     */
    public CommandVO(String name, String description) {
        // 设置命令名称
        this.name = name;
        // 设置命令描述
        this.description = description;
    }

    /**
     * 添加命令选项
     * 使用链式调用风格，方便连续添加多个选项
     *
     * @param optionVO 要添加的命令选项视图对象
     * @return 返回当前命令视图对象，支持链式调用
     */
    public CommandVO addOption(CommandOptionVO optionVO) {
        // 将选项添加到选项列表中
        this.options.add(optionVO);
        // 返回当前对象，支持链式调用
        return this;
    }

    /**
     * 添加命令参数
     * 使用链式调用风格，方便连续添加多个参数
     *
     * @param argumentVO 要添加的参数视图对象
     * @return 返回当前命令视图对象，支持链式调用
     */
    public CommandVO addArgument(ArgumentVO argumentVO) {
        // 将参数添加到参数列表中
        this.arguments.add(argumentVO);
        // 返回当前对象，支持链式调用
        return this;
    }

    /**
     * 获取CLI命令行接口对象
     *
     * @return CLI对象
     */
    public CLI cli() {
        return cli;
    }

    /**
     * 设置CLI命令行接口对象
     *
     * @param cli 要设置的CLI对象
     */
    public void setCli(CLI cli) {
        this.cli = cli;
    }

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置命令名称
     *
     * @param name 要设置的命令名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置命令描述
     *
     * @param description 要设置的命令描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取命令用法说明
     *
     * @return 命令用法说明
     */
    public String getUsage() {
        return usage;
    }

    /**
     * 设置命令用法说明
     *
     * @param usage 要设置的命令用法说明
     */
    public void setUsage(String usage) {
        this.usage = usage;
    }

    /**
     * 获取命令摘要
     *
     * @return 命令摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置命令摘要
     *
     * @param summary 要设置的命令摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取命令选项列表
     *
     * @return 命令选项列表
     */
    public List<CommandOptionVO> getOptions() {
        return options;
    }

    /**
     * 设置命令选项列表
     *
     * @param options 要设置的命令选项列表
     */
    public void setOptions(List<CommandOptionVO> options) {
        this.options = options;
    }

    /**
     * 获取命令参数列表
     *
     * @return 命令参数列表
     */
    public List<ArgumentVO> getArguments() {
        return arguments;
    }

    /**
     * 设置命令参数列表
     *
     * @param arguments 要设置的命令参数列表
     */
    public void setArguments(List<ArgumentVO> arguments) {
        this.arguments = arguments;
    }
}
