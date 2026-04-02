package com.taobao.arthas.core.command.basic1000;

import java.io.File;

import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 打印工作目录命令类
 * 用于返回当前工作目录的路径
 *
 * @author arthas
 */
@Name("pwd")
@Summary("Return working directory name")
public class PwdCommand extends AnnotatedCommand {
    /**
     * 处理pwd命令
     * 获取当前工作目录并返回
     * @param process 命令处理进程
     */
    @Override
    public void process(CommandProcess process) {
        // 获取当前工作目录的绝对路径
        String path = new File("").getAbsolutePath();
        // 将结果添加到进程
        process.appendResult(new PwdModel(path));
        // 结束处理
        process.end();
    }
}
