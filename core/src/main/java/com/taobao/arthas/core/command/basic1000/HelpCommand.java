package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.usage.StyledUsageFormatter;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

import static com.taobao.text.ui.Element.label;
import static com.taobao.text.ui.Element.row;

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
        String message;
        if (targetCmd == null) {
            message = RenderUtil.render(mainHelp(commands), process.width());
        } else {
            message = commandHelp(targetCmd, process.width());
        }
        process.write(message);
        process.end();
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

    private static String commandHelp(Command command, int width) {
        return StyledUsageFormatter.styledUsage(command.cli(), width);
    }

    private static Element mainHelp(List<Command> commands) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("NAME").style(Style.style(Decoration.bold)), new LabelElement("DESCRIPTION"));
        for (Command command : commands) {
            CLI cli = command.cli();
            // com.taobao.arthas.core.shell.impl.BuiltinCommandResolver doesn't have CLI instance
            if (cli == null || cli.isHidden()) {
                continue;
            }
            table.add(row().add(label(cli.getName()).style(Style.style(Color.green))).add(label(cli.getSummary())));
        }
        return table;
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
