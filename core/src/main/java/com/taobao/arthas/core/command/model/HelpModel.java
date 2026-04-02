package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.List;

/**
 * help命令的数据模型
 * 用于封装help命令的执行结果，可以返回命令列表或单个命令的详细信息
 *
 * @author gongdewei 2020/4/3
 */
public class HelpModel extends ResultModel {

    /**
     * 命令列表
     * 存储所有可用命令的简要信息，用于显示命令列表
     */
    private List<CommandVO> commands;

    /**
     * 详细命令信息
     * 存储单个命令的详细信息，用于显示特定命令的帮助文档
     */
    private CommandVO detailCommand;

    /**
     * 默认构造函数
     * 创建一个空的HelpModel实例
     */
    public HelpModel() {
    }

    /**
     * 构造函数 - 用于创建包含命令列表的模型
     *
     * @param commands 命令列表
     */
    public HelpModel(List<CommandVO> commands) {
        this.commands = commands;
    }

    /**
     * 构造函数 - 用于创建包含单个命令详细信息的模型
     *
     * @param command 单个命令的详细信息
     */
    public HelpModel(CommandVO command) {
        this.detailCommand = command;
    }

    /**
     * 添加命令到命令列表
     * 如果命令列表未初始化，会先创建一个新的ArrayList
     *
     * @param commandVO 要添加的命令对象
     */
    public void addCommandVO(CommandVO commandVO){
        // 如果命令列表为空，先创建一个新的ArrayList实例
        if (commands == null) {
            commands = new ArrayList<CommandVO>();
        }
        // 将命令添加到列表中
        this.commands.add(commandVO);
    }

    /**
     * 获取命令列表
     *
     * @return 命令列表
     */
    public List<CommandVO> getCommands() {
        return commands;
    }

    /**
     * 设置命令列表
     *
     * @param commands 命令列表
     */
    public void setCommands(List<CommandVO> commands) {
        this.commands = commands;
    }

    /**
     * 获取详细命令信息
     *
     * @return 单个命令的详细信息
     */
    public CommandVO getDetailCommand() {
        return detailCommand;
    }

    /**
     * 获取模型类型
     * 用于标识这是一个help命令的结果模型
     *
     * @return 模型类型标识符 "help"
     */
    @Override
    public String getType() {
        return "help";
    }
}
