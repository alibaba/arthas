package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.util.RenderUtil;

/**
 * 清屏命令类
 * 用于清除屏幕显示，类似于Unix的clear命令或Windows的cls命令
 * 只在tty（终端）会话中支持
 */
@Name("cls")
@Summary("Clear the screen")
public class ClsCommand extends AnnotatedCommand {
    /**
     * 处理清屏命令
     * @param process 命令处理进程
     */
    @Override
    public void process(CommandProcess process) {
        // 检查是否在tty会话中
        if (!process.session().isTty()) {
            // 非tty会话不支持清屏命令
            process.end(-1, "Command 'cls' is only support tty session.");
            return;
        }
        // 执行清屏操作并写入换行符
        process.write(RenderUtil.cls()).write("\n");
        // 命令执行成功
        process.end();
    }
}
