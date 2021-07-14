package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ResetModel;
import com.taobao.arthas.core.command.model.ShutdownModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * @author hengyunabc 2019-07-05
 */
@Name("stop")
@Summary("Stop/Shutdown Arthas server and exit the console.")
public class StopCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(StopCommand.class);
    @Override
    public void process(CommandProcess process) {
        shutdown(process);
    }
    private static void shutdown(CommandProcess process) {
        ArthasBootstrap arthasBootstrap = ArthasBootstrap.getInstance();
        try {
            // 退出之前需要重置所有的增强类
            process.appendResult(new MessageModel("Resetting all enhanced classes ..."));
            EnhancerAffect enhancerAffect = arthasBootstrap.reset();
            process.appendResult(new ResetModel(enhancerAffect));
            process.appendResult(new ShutdownModel(true, "Arthas Server is going to shutdown..."));
        } catch (Throwable e) {
            logger.error("An error occurred when stopping arthas server.", e);
            process.appendResult(new ShutdownModel(false, "An error occurred when stopping arthas server."));
        } finally {
            process.end();
            arthasBootstrap.destroy();
        }
    }
}
