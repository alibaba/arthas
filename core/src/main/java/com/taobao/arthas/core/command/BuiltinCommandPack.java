package com.taobao.arthas.core.command;


import java.util.ArrayList;
import java.util.List;

import com.taobao.arthas.core.command.basic1000.*;
import com.taobao.arthas.core.command.hidden.JulyCommand;
import com.taobao.arthas.core.command.hidden.OptionsCommand;
import com.taobao.arthas.core.command.hidden.ThanksCommand;
import com.taobao.arthas.core.command.klass100.*;
import com.taobao.arthas.core.command.monitor200.*;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;

/**
 * TODO automatically discover the built-in commands.
 * @author beiwei30 on 17/11/2016.
 */
public class BuiltinCommandPack implements CommandResolver {

    private static List<Command> commands = new ArrayList<Command>();

    static {
        initCommands();
    }

    @Override
    public List<Command> commands() {
        return commands;
    }

    private static void initCommands() {
        commands.add(Command.create(HelpCommand.class));
        commands.add(Command.create(KeymapCommand.class));
        commands.add(Command.create(SearchClassCommand.class));
        commands.add(Command.create(SearchMethodCommand.class));
        commands.add(Command.create(ClassLoaderCommand.class));
        commands.add(Command.create(JadCommand.class));
        commands.add(Command.create(GetStaticCommand.class));
        commands.add(Command.create(MonitorCommand.class));
        commands.add(Command.create(StackCommand.class));
        commands.add(Command.create(ThreadCommand.class));
        commands.add(Command.create(TraceCommand.class));
        commands.add(Command.create(WatchCommand.class));
        commands.add(Command.create(TimeTunnelCommand.class));
        commands.add(Command.create(JvmCommand.class));
        // commands.add(Command.create(GroovyScriptCommand.class));
        commands.add(Command.create(OgnlCommand.class));
        commands.add(Command.create(MemoryCompilerCommand.class));
        commands.add(Command.create(RedefineCommand.class));
        commands.add(Command.create(DashboardCommand.class));
        commands.add(Command.create(DumpClassCommand.class));
        commands.add(Command.create(JulyCommand.class));
        commands.add(Command.create(ThanksCommand.class));
        commands.add(Command.create(OptionsCommand.class));
        commands.add(Command.create(ClsCommand.class));
        commands.add(Command.create(ResetCommand.class));
        commands.add(Command.create(VersionCommand.class));
        commands.add(Command.create(ShutdownCommand.class));
        commands.add(Command.create(SessionCommand.class));
        commands.add(Command.create(SystemPropertyCommand.class));
        commands.add(Command.create(SystemEnvCommand.class));
        commands.add(Command.create(HistoryCommand.class));
        commands.add(Command.create(CatCommand.class));
        commands.add(Command.create(PwdCommand.class));
        commands.add(Command.create(MBeanCommand.class));
        commands.add(Command.create(DnsCommand.class));
    }
}
