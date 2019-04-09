package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * 关闭命令
 *
 * @author vlinux on 14/10/23.
 */
@Name("shutdown")
@Summary("Shutdown Arthas server and exit the console")
public class ShutdownCommand extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        try {
            // 退出之前需要重置所有的增强类
            Instrumentation inst = process.session().getInstrumentation();
            EnhancerAffect enhancerAffect = Enhancer.reset(inst, new WildcardMatcher("*"));
            process.write(enhancerAffect.toString()).write("\n");
            process.write("Arthas Server is going to shut down...\n");
        } catch (UnmodifiableClassException e) {
            // ignore
        } finally {
            process.end();
            ShellServer server = process.session().getServer();
            server.close();
        }
    }
}
