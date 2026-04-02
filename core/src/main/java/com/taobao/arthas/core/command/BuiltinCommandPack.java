package com.taobao.arthas.core.command;

import java.util.ArrayList;
import java.util.List;


import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.basic1000.*;
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
import com.taobao.arthas.core.command.monitor200.MemoryCommand;
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
 * 内置命令打包器
 * 负责收集和管理所有Arthas内置命令，支持禁用某些命令
 *
 * TODO: 自动发现内置命令
 * @author beiwei30 on 17/11/2016.
 */
public class BuiltinCommandPack implements CommandResolver {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(BuiltinCommandPack.class);

    // 命令列表，存储所有可用的内置命令
    private List<Command> commands = new ArrayList<Command>();

    /**
     * 构造函数
     * @param disabledCommands 禁用的命令列表
     */
    public BuiltinCommandPack(List<String> disabledCommands) {
        initCommands(disabledCommands);
    }

    /**
     * 获取所有命令
     * @return 命令列表
     */
    @Override
    public List<Command> commands() {
        return commands;
    }

    /**
     * 初始化命令列表
     * 将所有内置命令类注册到命令列表中，排除被禁用的命令
     *
     * @param disabledCommands 禁用的命令名称列表
     */
    private void initCommands(List<String> disabledCommands) {
        // 创建命令类列表，初始容量为33（已知的大概命令数量）
        List<Class<? extends AnnotatedCommand>> commandClassList = new ArrayList<Class<? extends AnnotatedCommand>>(33);

        // 基础命令
        commandClassList.add(HelpCommand.class);          // 帮助命令
        commandClassList.add(AuthCommand.class);          // 认证命令
        commandClassList.add(KeymapCommand.class);        // 键盘映射命令

        // 类相关命令
        commandClassList.add(SearchClassCommand.class);   // 搜索类命令
        commandClassList.add(SearchMethodCommand.class);  // 搜索方法命令
        commandClassList.add(ClassLoaderCommand.class);   // 类加载器命令
        commandClassList.add(JadCommand.class);           // 反编译命令
        commandClassList.add(GetStaticCommand.class);     // 获取静态变量命令
        commandClassList.add(DumpClassCommand.class);     // 转储类命令

        // 增强命令
        commandClassList.add(MonitorCommand.class);       // 监控方法执行命令
        commandClassList.add(StackCommand.class);         // 查看方法调用栈命令
        commandClassList.add(ThreadCommand.class);        // 线程查看命令
        commandClassList.add(TraceCommand.class);         // 跟踪方法调用路径命令
        commandClassList.add(WatchCommand.class);         // 观察方法执行详情命令
        commandClassList.add(TimeTunnelCommand.class);    // 时光隧道命令（查看历史调用）

        // JVM相关命令
        commandClassList.add(JvmCommand.class);           // JVM信息命令
        commandClassList.add(MemoryCommand.class);        // 内存信息命令
        commandClassList.add(PerfCounterCommand.class);   // 性能计数器命令
        // Groovy脚本命令（已注释，默认不启用）
        // commandClassList.add(GroovyScriptCommand.class);
        commandClassList.add(OgnlCommand.class);          // OGNL表达式执行命令
        commandClassList.add(MemoryCompilerCommand.class);// 内存编译命令
        commandClassList.add(RedefineCommand.class);      // 重定义类命令
        commandClassList.add(RetransformCommand.class);   // 重新转换类命令
        commandClassList.add(DashboardCommand.class);     // 仪表盘命令
        commandClassList.add(HeapDumpCommand.class);      // 堆转储命令

        // 隐藏的彩蛋命令
        commandClassList.add(JulyCommand.class);          // 七月彩蛋命令
        commandClassList.add(ThanksCommand.class);        // 感谢彩蛋命令

        // 系统命令
        commandClassList.add(OptionsCommand.class);       // 选项命令
        commandClassList.add(ClsCommand.class);           // 清屏命令
        commandClassList.add(ResetCommand.class);         // 重置命令
        commandClassList.add(VersionCommand.class);       // 版本命令
        commandClassList.add(SessionCommand.class);       // 会话命令
        commandClassList.add(SystemPropertyCommand.class);// 系统属性命令
        commandClassList.add(SystemEnvCommand.class);     // 系统环境变量命令
        commandClassList.add(VMOptionCommand.class);      // JVM选项命令
        commandClassList.add(LoggerCommand.class);        // 日志命令
        commandClassList.add(HistoryCommand.class);       // 历史命令
        commandClassList.add(CatCommand.class);           // 查看文件命令
        commandClassList.add(Base64Command.class);        // Base64编解码命令
        commandClassList.add(EchoCommand.class);          // 输出命令
        commandClassList.add(PwdCommand.class);           // 当前目录命令
        commandClassList.add(MBeanCommand.class);         // MBean管理命令
        commandClassList.add(GrepCommand.class);          // 文本搜索命令
        commandClassList.add(TeeCommand.class);           // tee命令
        commandClassList.add(ProfilerCommand.class);      // 性能分析命令
        commandClassList.add(VmToolCommand.class);        // VM工具命令
        commandClassList.add(StopCommand.class);          // 停止命令

        // 尝试添加JFR命令（Java Flight Recorder，仅在某些JDK版本可用）
        try {
            // 检查JDK是否支持JFR（通过检查Recording类是否存在）
            if (ClassLoader.getSystemClassLoader().getResource("jdk/jfr/Recording.class") != null) {
                commandClassList.add(JFRCommand.class);
            }
        } catch (Throwable e) {
            // 如果当前JDK版本不支持JFR命令，记录错误日志
            logger.error("This jdk version not support jfr command");
        }

        // 遍历所有命令类，创建命令实例并添加到命令列表中
        for (Class<? extends AnnotatedCommand> clazz : commandClassList) {
            // 获取命令类上的@Name注解
            Name name = clazz.getAnnotation(Name.class);
            if (name != null && name.value() != null) {
                // 如果该命令在禁用列表中，跳过它
                if (disabledCommands.contains(name.value())) {
                    continue;
                }
            }
            // 创建命令实例并添加到命令列表
            commands.add(Command.create(clazz));
        }
    }
}
