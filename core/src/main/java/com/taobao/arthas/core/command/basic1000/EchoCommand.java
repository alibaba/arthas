package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.EchoModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Echo命令类
 * 用于将参数输出到标准输出，类似于Unix/Linux的echo命令
 *
 * @author hengyunabc
 *
 */
@Name("echo")
@Summary("write arguments to the standard output")
@Description("\nExamples:\n" +
        "  echo 'abc'\n" +
        Constants.WIKI + Constants.WIKI_HOME + "echo")
public class EchoCommand extends AnnotatedCommand {
    // 要输出的消息内容
    private String message;

    /**
     * 设置要输出的消息
     *
     * @param message 消息内容
     */
    @Argument(argName = "message", index = 0, required = false)
    @Description("message")
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 处理命令执行
     * 将消息内容添加到处理结果中，并结束命令处理
     *
     * @param process 命令处理进程对象
     */
    @Override
    public void process(CommandProcess process) {
        // 如果消息不为空，则创建EchoModel并添加到处理结果中
        if (message != null) {
            process.appendResult(new EchoModel(message));
        }

        // 结束命令处理
        process.end();
    }

}
