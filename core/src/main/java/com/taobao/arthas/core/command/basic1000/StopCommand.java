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
 * 停止命令类，用于停止/关闭Arthas服务器并退出控制台<br/>
 *
 * 该命令会执行以下操作：
 * 1. 重置所有被Arthas增强的类，恢复到原始状态
 * 2. 显示重置操作的统计信息
 * 3. 关闭Arthas服务器
 * 4. 清理相关资源
 *
 * 在停止之前会自动重置所有增强的类，确保应用程序恢复到未增强的状态。
 *
 * @author hengyunabc 2019-07-05
 */
@Name("stop")
@Summary("Stop/Shutdown Arthas server and exit the console.")
public class StopCommand extends AnnotatedCommand {
    /**
     * 日志记录器，用于记录停止过程中的日志信息
     */
    private static final Logger logger = LoggerFactory.getLogger(StopCommand.class);

    /**
     * 处理stop命令的执行逻辑
     * 调用shutdown方法执行实际的停止操作
     *
     * @param process 命令处理进程对象，包含会话信息和执行上下文
     */
    @Override
    public void process(CommandProcess process) {
        shutdown(process);
    }

    /**
     * 执行Arthas服务器的关闭操作
     *
     * 该方法会按以下步骤执行关闭操作：
     * 1. 重置所有被增强的类
     * 2. 显示重置结果
     * 3. 发送关闭消息
     * 4. 清理资源并销毁ArthasBootstrap实例
     *
     * @param process 命令处理进程对象，用于输出关闭过程的信息
     */
    private static void shutdown(CommandProcess process) {
        // 获取ArthasBootstrap单例实例
        ArthasBootstrap arthasBootstrap = ArthasBootstrap.getInstance();
        try {
            // 退出之前需要重置所有的增强类，恢复到原始状态
            // 发送重置开始的消息
            process.appendResult(new MessageModel("Resetting all enhanced classes ..."));

            // 执行重置操作，enhancerAffect包含重置的统计信息
            EnhancerAffect enhancerAffect = arthasBootstrap.reset();

            // 发送重置结果，包含重置的类数量等统计信息
            process.appendResult(new ResetModel(enhancerAffect));

            // 发送关闭成功消息
            process.appendResult(new ShutdownModel(true, "Arthas Server is going to shutdown..."));
        } catch (Throwable e) {
            // 如果在停止过程中发生错误，记录错误日志
            logger.error("An error occurred when stopping arthas server.", e);

            // 发送关闭失败消息
            process.appendResult(new ShutdownModel(false, "An error occurred when stopping arthas server."));
        } finally {
            // 无论成功与否，都要执行以下清理操作
            // 结束命令处理进程
            process.end();

            // 销毁ArthasBootstrap实例，释放所有资源
            arthasBootstrap.destroy();
        }
    }
}
