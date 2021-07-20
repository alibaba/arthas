package com.taobao.arthas.core.command;

import java.util.ArrayList;
import java.util.List;

import com.taobao.arthas.core.command.basic1000.AuthCommand;
import com.taobao.arthas.core.command.basic1000.Base64Command;
import com.taobao.arthas.core.command.basic1000.CatCommand;
import com.taobao.arthas.core.command.basic1000.ClsCommand;
import com.taobao.arthas.core.command.basic1000.EchoCommand;
import com.taobao.arthas.core.command.basic1000.GrepCommand;
import com.taobao.arthas.core.command.basic1000.HelpCommand;
import com.taobao.arthas.core.command.basic1000.HistoryCommand;
import com.taobao.arthas.core.command.basic1000.KeymapCommand;
import com.taobao.arthas.core.command.basic1000.OptionsCommand;
import com.taobao.arthas.core.command.basic1000.PwdCommand;
import com.taobao.arthas.core.command.basic1000.ResetCommand;
import com.taobao.arthas.core.command.basic1000.SessionCommand;
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
import com.taobao.arthas.core.command.monitor200.VmToolCommand;
import com.taobao.arthas.core.command.monitor200.WatchCommand;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.middleware.cli.annotations.Name;

/**
 * TODO automatically discover the built-in commands.
 * @author beiwei30 on 17/11/2016.
 */
public class BuiltinCommandPack implements CommandResolver {

    private List<Command> commands = new ArrayList<Command>();

    public BuiltinCommandPack(List<String> disabledCommands) {
        initCommands(disabledCommands);
    }

    @Override
    public List<Command> commands() {
        return commands;
    }

    private void initCommands(List<String> disabledCommands) {
        List<Class<? extends AnnotatedCommand>> commandClassList = new ArrayList<Class<? extends AnnotatedCommand>>(32);
        commandClassList.add(HelpCommand.class);
        commandClassList.add(AuthCommand.class);
        commandClassList.add(KeymapCommand.class);
        commandClassList.add(SearchClassCommand.class);
        commandClassList.add(SearchMethodCommand.class);
        commandClassList.add(ClassLoaderCommand.class);
        commandClassList.add(JadCommand.class);
        commandClassList.add(GetStaticCommand.class);
        commandClassList.add(MonitorCommand.class);
        commandClassList.add(StackCommand.class);
        commandClassList.add(ThreadCommand.class);
        commandClassList.add(TraceCommand.class);
        commandClassList.add(WatchCommand.class);
        commandClassList.add(TimeTunnelCommand.class);
        commandClassList.add(JvmCommand.class);
        commandClassList.add(PerfCounterCommand.class);
        // commandClassList.add(GroovyScriptCommand.class);
        commandClassList.add(OgnlCommand.class);
        commandClassList.add(MemoryCompilerCommand.class);
        commandClassList.add(RedefineCommand.class);
        commandClassList.add(RetransformCommand.class);
        commandClassList.add(DashboardCommand.class);
        commandClassList.add(DumpClassCommand.class);
        commandClassList.add(HeapDumpCommand.class);
        commandClassList.add(JulyCommand.class);
        commandClassList.add(ThanksCommand.class);
        commandClassList.add(OptionsCommand.class);
        commandClassList.add(ClsCommand.class);
        commandClassList.add(ResetCommand.class);
        commandClassList.add(VersionCommand.class);
        commandClassList.add(SessionCommand.class);
        commandClassList.add(SystemPropertyCommand.class);
        commandClassList.add(SystemEnvCommand.class);
        commandClassList.add(VMOptionCommand.class);
        commandClassList.add(LoggerCommand.class);
        commandClassList.add(HistoryCommand.class);
        commandClassList.add(CatCommand.class);
        commandClassList.add(Base64Command.class);
        commandClassList.add(EchoCommand.class);
        commandClassList.add(PwdCommand.class);
        commandClassList.add(MBeanCommand.class);
        commandClassList.add(GrepCommand.class);
        commandClassList.add(TeeCommand.class);
        commandClassList.add(ProfilerCommand.class);
        commandClassList.add(VmToolCommand.class);
        commandClassList.add(StopCommand.class);

        for (Class<? extends AnnotatedCommand> clazz : commandClassList) {
            Name name = clazz.getAnnotation(Name.class);
            if (name != null && name.value() != null) {
                if (disabledCommands.contains(name.value())) {
                    continue;
                }
            }
            commands.add(Command.create(clazz));
        }
    }
}
