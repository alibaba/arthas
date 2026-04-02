package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.model.ArgumentVO;
import com.taobao.arthas.core.command.model.CommandOptionVO;
import com.taobao.arthas.core.command.model.CommandVO;
import com.taobao.arthas.core.command.model.HelpModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.usage.StyledUsageFormatter;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import java.util.ArrayList;
import java.util.List;

/**
 * Help命令类
 * 用于显示Arthas命令的帮助信息
 * 可以显示所有命令列表，也可以显示特定命令的详细帮助
 *
 * @author vlinux on 14/10/26.
 */
@Name("help")
@Summary("Display Arthas Help")
@Description("Examples:\n" + " help\n" + " help sc\n" + " help sm\n" + " help watch")
public class HelpCommand extends AnnotatedCommand {

    // 要查询的命令名称
    private String cmd;

    /**
     * 设置要查询的命令名称
     *
     * @param cmd 命令名称
     */
    @Argument(index = 0, argName = "cmd", required = false)
    @Description("command name")
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    /**
     * 处理命令执行
     * 如果指定了命令名称，显示该命令的详细帮助；否则显示所有命令列表
     *
     * @param process 命令处理进程对象
     */
    @Override
    public void process(CommandProcess process) {
        // 获取所有可用命令
        List<Command> commands = allCommands(process.session());
        // 查找目标命令
        Command targetCmd = findCommand(commands);
        // 如果未找到目标命令，显示所有命令列表；否则显示目标命令的详细帮助
        if (targetCmd == null) {
            process.appendResult(createHelpModel(commands));
        } else {
            process.appendResult(createHelpDetailModel(targetCmd));
        }
        // 结束命令处理
        process.end();
    }

    /**
     * 创建命令详细帮助模型
     *
     * @param targetCmd 目标命令
     * @return 帮助模型对象
     */
    public HelpModel createHelpDetailModel(Command targetCmd) {
        return new HelpModel(createCommandVO(targetCmd, true));
    }

    /**
     * 创建所有命令的帮助模型
     *
     * @param commands 所有命令列表
     * @return 帮助模型对象
     */
    private HelpModel createHelpModel(List<Command> commands) {
        HelpModel helpModel = new HelpModel();
        // 遍历所有命令，创建命令视图对象
        for (Command command : commands) {
            // 跳过没有CLI或隐藏的命令
            if(command.cli() == null || command.cli().isHidden()){
                continue;
            }
            // 添加命令视图对象（不包含详细信息）
            helpModel.addCommandVO(createCommandVO(command, false));
        }
        return helpModel;
    }

    /**
     * 创建命令视图对象
     *
     * @param command 命令对象
     * @param withDetail 是否包含详细信息
     * @return 命令视图对象
     */
    private CommandVO createCommandVO(Command command, boolean withDetail) {
        CLI cli = command.cli();
        CommandVO commandVO = new CommandVO();
        // 设置命令名称
        commandVO.setName(command.name());
        if (cli!=null){
            // 设置命令摘要
            commandVO.setSummary(cli.getSummary());
            if (withDetail){
                // 如果需要详细信息，设置CLI、使用方法、描述、选项和参数
                commandVO.setCli(cli);
                // 创建使用格式化器并计算使用行
                StyledUsageFormatter usageFormatter = new StyledUsageFormatter(null);
                String usageLine = usageFormatter.computeUsageLine(null, cli);
                commandVO.setUsage(usageLine);
                // 设置命令描述
                commandVO.setDescription(cli.getDescription());

                // 以线程安全的方式遍历选项列表
                List<Option> options = cli.getOptions();
                for (int i = 0; i < options.size(); i++) {
                    Option option = options.get(i);
                    // 跳过隐藏的选项
                    if (option.isHidden()){
                        continue;
                    }
                    // 添加选项视图对象
                    commandVO.addOption(createOptionVO(option));
                }

                // 处理参数列表
                List<com.taobao.middleware.cli.Argument> arguments = cli.getArguments();
                for (int i = 0; i < arguments.size(); i++) {
                    com.taobao.middleware.cli.Argument argument = arguments.get(i);
                    // 跳过隐藏的参数
                    if (argument.isHidden()){
                        continue;
                    }
                    // 添加参数视图对象
                    commandVO.addArgument(createArgumentVO(argument));
                }
            }
        }
        return commandVO;
    }

    /**
     * 创建参数视图对象
     *
     * @param argument 参数对象
     * @return 参数视图对象
     */
    private ArgumentVO createArgumentVO(com.taobao.middleware.cli.Argument argument) {
        ArgumentVO argumentVO = new ArgumentVO();
        // 设置参数名称
        argumentVO.setArgName(argument.getArgName());
        // 设置是否多值
        argumentVO.setMultiValued(argument.isMultiValued());
        // 设置是否必填
        argumentVO.setRequired(argument.isRequired());
        return argumentVO;
    }

    /**
     * 创建选项视图对象
     *
     * @param option 选项对象
     * @return 选项视图对象
     */
    private CommandOptionVO createOptionVO(Option option) {
        CommandOptionVO optionVO = new CommandOptionVO();
        // 设置长选项名（如果存在且不为空）
        if (!isEmptyName(option.getLongName())) {
            optionVO.setLongName(option.getLongName());
        }
        // 设置短选项名（如果存在且不为空）
        if (!isEmptyName(option.getShortName())) {
            optionVO.setShortName(option.getShortName());
        }
        // 设置选项描述
        optionVO.setDescription(option.getDescription());
        // 设置是否接受值
        optionVO.setAcceptValue(option.acceptValue());
        return optionVO;
    }

    /**
     * 判断选项名是否为空
     *
     * @param name 选项名称
     * @return 如果名称为null或等于NO_NAME则返回true，否则返回false
     */
    private boolean isEmptyName(String name) {
        return name == null || name.equals(Option.NO_NAME);
    }

    /**
     * 命令自动补全
     * 当用户输入help命令后，可以自动补全可用的命令名称
     *
     * @param completion 补全对象
     */
    @Override
    public void complete(Completion completion) {
        // 获取所有命令
        List<Command> commands = allCommands(completion.session());

        // 收集所有命令名称
        List<String> names = new ArrayList<String>(commands.size());
        for (Command command : commands) {
            CLI cli = command.cli();
            // 跳过没有CLI或隐藏的命令
            if (cli == null || cli.isHidden()) {
                continue;
            }
            // 添加命令名称到列表
            names.add(command.name());
        }
        // 执行补全
        CompletionUtils.complete(completion, names);
    }

    /**
     * 获取会话中的所有命令
     *
     * @param session 会话对象
     * @return 所有命令列表
     */
    private List<Command> allCommands(Session session) {
        // 获取所有命令解析器
        List<CommandResolver> commandResolvers = session.getCommandResolvers();
        List<Command> commands = new ArrayList<Command>();
        // 遍历命令解析器，收集所有命令
        for (CommandResolver commandResolver : commandResolvers) {
            commands.addAll(commandResolver.commands());
        }
        return commands;
    }

    /**
     * 在命令列表中查找指定名称的命令
     *
     * @param commands 命令列表
     * @return 找到的命令对象，如果未找到则返回null
     */
    private Command findCommand(List<Command> commands) {
        // 遍历命令列表，查找匹配的命令
        for (Command command : commands) {
            if (command.name().equals(cmd)) {
                return command;
            }
        }
        return null;
    }
}
