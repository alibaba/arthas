package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.vdian.vclub.MemoryAnalyzer;
import com.vdian.vclub.MemoryInfo;

import java.util.List;

/**
 * Memory command
 *
 * @author zhangzicheng
 * @date 2021/01/16
 */
@Name("memory")
@Summary("detailed display of memory usage without dump")
@Description("\nExamples:\n" + "  memory\n" + "  memory --all\n"
        + Constants.WIKI + Constants.WIKI_HOME + "memory")
public class MemoryCommand extends AnnotatedCommand {

    private static final Logger logger = LoggerFactory.getLogger(MemoryCommand.class);

    boolean all = false;

    @Option(shortName = "a", longName = "all", flag = true)
    @Description("show all information, even if the memory usage is small")
    public void setAll(boolean all) {
        this.all = all;
    }

    @Override
    public void process(CommandProcess process) {
        process.appendResult(new MessageModel("Memory analyzing ..."));
        try {
            List<MemoryInfo> memoryInfos = MemoryAnalyzer.analyze(all);
            process.appendResult(new MessageModel("Memory analyze finished"));
            for (MemoryInfo memoryInfo : memoryInfos) {
                process.appendResult(new MessageModel(memoryInfo.toString()));
            }
            process.appendResult(new MessageModel("all information displayed\n"));
            process.end();
        } catch (Throwable t) {
            String errorMsg = "memory analyze failed: " + t.getMessage();
            logger.error(errorMsg, t);
            process.end(-1, errorMsg);
        }
    }
}
