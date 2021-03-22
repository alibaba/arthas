package com.taobao.arthas.core.command;

import com.taobao.arthas.core.command.basic1000.Base64Command;
import com.taobao.arthas.core.command.basic1000.CatCommand;
import com.taobao.arthas.core.command.basic1000.ClsCommand;
import com.taobao.arthas.core.command.basic1000.EchoCommand;
import com.taobao.arthas.core.command.basic1000.GrepCommand;
import com.taobao.arthas.core.command.basic1000.HelpCommand;
import com.taobao.arthas.core.command.basic1000.HistoryCommand;
import com.taobao.arthas.core.command.basic1000.KeymapCommand;
import com.taobao.arthas.core.command.basic1000.OptionsCommand;
import com.taobao.arthas.core.command.basic1000.AuthCommand;
import com.taobao.arthas.core.command.basic1000.PwdCommand;
import com.taobao.arthas.core.command.basic1000.ResetCommand;
import com.taobao.arthas.core.command.basic1000.SessionCommand;
import com.taobao.arthas.core.command.basic1000.ShutdownCommand;
import com.taobao.arthas.core.command.basic1000.StopCommand;
import com.taobao.arthas.core.command.basic1000.SystemEnvCommand;
import com.taobao.arthas.core.command.basic1000.SystemPropertyCommand;
import com.taobao.arthas.core.command.basic1000.TeeCommand;
import com.taobao.arthas.core.command.basic1000.VMOptionCommand;
import com.taobao.arthas.core.command.basic1000.VersionCommand;
import com.taobao.arthas.core.command.hidden.JulyCommand;
import com.taobao.arthas.core.command.hidden.ThanksCommand;
import com.taobao.arthas.core.command.klass100.ClassLoaderCommand;
import com.taobao.arthas.core.command.klass100.DumpClassCommand;
import com.taobao.arthas.core.command.klass100.GetStaticCommand;
import com.taobao.arthas.core.command.klass100.JadCommand;
import com.taobao.arthas.core.command.klass100.MemoryCompilerCommand;
import com.taobao.arthas.core.command.klass100.OgnlCommand;
import com.taobao.arthas.core.command.klass100.RedefineCommand;
import com.taobao.arthas.core.command.klass100.RetransformCommand;
import com.taobao.arthas.core.command.klass100.SearchClassCommand;
import com.taobao.arthas.core.command.klass100.SearchMethodCommand;
import com.taobao.arthas.core.command.logger.LoggerCommand;
import com.taobao.arthas.core.command.monitor200.DashboardCommand;
import com.taobao.arthas.core.command.monitor200.HeapDumpCommand;
import com.taobao.arthas.core.command.monitor200.JvmCommand;
import com.taobao.arthas.core.command.monitor200.MBeanCommand;
import com.taobao.arthas.core.command.monitor200.MonitorCommand;
import com.taobao.arthas.core.command.monitor200.PerfCounterCommand;
import com.taobao.arthas.core.command.monitor200.ProfilerCommand;
import com.taobao.arthas.core.command.monitor200.StackCommand;
import com.taobao.arthas.core.command.monitor200.ThreadCommand;
import com.taobao.arthas.core.command.monitor200.TimeTunnelCommand;
import com.taobao.arthas.core.command.monitor200.TraceCommand;
import com.taobao.arthas.core.command.monitor200.WatchCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;

import java.util.ArrayList;
import java.util.List;

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
        commands.add(Command.create(AuthCommand.class));
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
        commands.add(Command.create(PerfCounterCommand.class));
        // commands.add(Command.create(GroovyScriptCommand.class));
        commands.add(Command.create(OgnlCommand.class));
        commands.add(Command.create(MemoryCompilerCommand.class));
        commands.add(Command.create(RedefineCommand.class));
        commands.add(Command.create(RetransformCommand.class));
        commands.add(Command.create(DashboardCommand.class));
        commands.add(Command.create(DumpClassCommand.class));
        commands.add(Command.create(HeapDumpCommand.class));
        commands.add(Command.create(JulyCommand.class));
        commands.add(Command.create(ThanksCommand.class));
        commands.add(Command.create(OptionsCommand.class));
        commands.add(Command.create(ClsCommand.class));
        commands.add(Command.create(ResetCommand.class));
        commands.add(Command.create(VersionCommand.class));
        commands.add(Command.create(SessionCommand.class));
        commands.add(Command.create(SystemPropertyCommand.class));
        commands.add(Command.create(SystemEnvCommand.class));
        commands.add(Command.create(VMOptionCommand.class));
        commands.add(Command.create(LoggerCommand.class));
        commands.add(Command.create(HistoryCommand.class));
        commands.add(Command.create(CatCommand.class));
        commands.add(Command.create(Base64Command.class));
        commands.add(Command.create(EchoCommand.class));
        commands.add(Command.create(PwdCommand.class));
        commands.add(Command.create(MBeanCommand.class));
        commands.add(Command.create(GrepCommand.class));
        commands.add(Command.create(TeeCommand.class));
        commands.add(Command.create(ProfilerCommand.class));
        commands.add(Command.create(ShutdownCommand.class));
        commands.add(Command.create(StopCommand.class));
    }
}
