package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.Hidden;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * 关闭命令
 *
 * @author vlinux on 14/10/23.
 * @see StopCommand
 */
@Name("shutdown")
@Summary("Shutdown Arthas server and exit the console")
@Hidden
public class ShutdownCommand extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        shutdown(process);
    }

    public static void shutdown(CommandProcess process) {
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
            ShellServer server = ArthasBootstrap.getInstance().getShellServer();
            if (server != null) {
                server.close();
            }

            SessionManager sessionManager = ArthasBootstrap.getInstance().getSessionManager();
            if (sessionManager != null){
                sessionManager.close();
            }
        }
    }
}
