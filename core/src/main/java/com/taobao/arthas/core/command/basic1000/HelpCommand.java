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
 * @author vlinux on 14/10/26.
 */
@Name("help")
@Summary("Display Arthas Help")
@Description("Examples:\n" + " help\n" + " help sc\n" + " help sm\n" + " help watch")
public class HelpCommand extends AnnotatedCommand {

    private String cmd;

    @Argument(index = 0, argName = "cmd", required = false)
    @Description("command name")
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public void process(CommandProcess process) {
        List<Command> commands = allCommands(process.session());
        Command targetCmd = findCommand(commands);
        if (targetCmd == null) {
            process.appendResult(createHelpModel(commands));
        } else {
            process.appendResult(createHelpDetailModel(targetCmd));
        }
        process.end();
    }

    public HelpModel createHelpDetailModel(Command targetCmd) {
        return new HelpModel(createCommandVO(targetCmd, true));
    }

    private HelpModel createHelpModel(List<Command> commands) {
        HelpModel helpModel = new HelpModel();
        for (Command command : commands) {
            if(command.cli() == null || command.cli().isHidden()){
                continue;
            }
            helpModel.addCommandVO(createCommandVO(command, false));
        }
        return helpModel;
    }

    private CommandVO createCommandVO(Command command, boolean withDetail) {
        CLI cli = command.cli();
        CommandVO commandVO = new CommandVO();
        commandVO.setName(command.name());
        if (cli!=null){
            commandVO.setSummary(cli.getSummary());
            if (withDetail){
                commandVO.setCli(cli);
                StyledUsageFormatter usageFormatter = new StyledUsageFormatter(null);
                String usageLine = usageFormatter.computeUsageLine(null, cli);
                commandVO.setUsage(usageLine);
                commandVO.setDescription(cli.getDescription());

                //以线程安全的方式遍历options
                List<Option> options = cli.getOptions();
                for (int i = 0; i < options.size(); i++) {
                    Option option = options.get(i);
                    if (option.isHidden()){
                        continue;
                    }
                    commandVO.addOption(createOptionVO(option));
                }

                //arguments
                List<com.taobao.middleware.cli.Argument> arguments = cli.getArguments();
                for (int i = 0; i < arguments.size(); i++) {
                    com.taobao.middleware.cli.Argument argument = arguments.get(i);
                    if (argument.isHidden()){
                        continue;
                    }
                    commandVO.addArgument(createArgumentVO(argument));
                }
            }
        }
        return commandVO;
    }

    private ArgumentVO createArgumentVO(com.taobao.middleware.cli.Argument argument) {
        ArgumentVO argumentVO = new ArgumentVO();
        argumentVO.setArgName(argument.getArgName());
        argumentVO.setMultiValued(argument.isMultiValued());
        argumentVO.setRequired(argument.isRequired());
        return argumentVO;
    }

    private CommandOptionVO createOptionVO(Option option) {
        CommandOptionVO optionVO = new CommandOptionVO();
        if (!isEmptyName(option.getLongName())) {
            optionVO.setLongName(option.getLongName());
        }
        if (!isEmptyName(option.getShortName())) {
            optionVO.setShortName(option.getShortName());
        }
        optionVO.setDescription(option.getDescription());
        optionVO.setAcceptValue(option.acceptValue());
        return optionVO;
    }

    private boolean isEmptyName(String name) {
        return name == null || name.equals(Option.NO_NAME);
    }

    @Override
    public void complete(Completion completion) {
        List<Command> commands = allCommands(completion.session());

        List<String> names = new ArrayList<String>(commands.size());
        for (Command command : commands) {
            CLI cli = command.cli();
            if (cli == null || cli.isHidden()) {
                continue;
            }
            names.add(command.name());
        }
        CompletionUtils.complete(completion, names);
    }

    private List<Command> allCommands(Session session) {
        List<CommandResolver> commandResolvers = session.getCommandResolvers();
        List<Command> commands = new ArrayList<Command>();
        for (CommandResolver commandResolver : commandResolvers) {
            commands.addAll(commandResolver.commands());
        }
        return commands;
    }

    private Command findCommand(List<Command> commands) {
        for (Command command : commands) {
            if (command.name().equals(cmd)) {
                return command;
            }
        }
        return null;
    }
}
