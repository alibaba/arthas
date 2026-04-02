package com.taobao.arthas.core.command.basic1000;


import com.taobao.arthas.core.command.model.VersionModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 版本命令类
 *
 * 用于显示Arthas的版本信息。
 * 该命令会读取Arthas的版本号并返回给用户，帮助用户确认当前使用的Arthas版本。
 *
 * 使用示例：
 * - version: 显示当前Arthas版本
 *
 * @author vlinux
 */
@Name("version")
@Summary("Display Arthas version")
public class VersionCommand extends AnnotatedCommand {

    /**
     * 处理命令执行逻辑
     *
     * 创建版本模型对象，设置Arthas版本号，并将结果添加到进程输出中。
     *
     * @param process 命令处理进程对象，用于输出结果和控制命令流程
     */
    @Override
    public void process(CommandProcess process) {
        // 创建版本模型对象
        VersionModel result = new VersionModel();
        // 从ArthasBanner工具类获取版本号并设置到模型中
        result.setVersion(ArthasBanner.version());
        // 将结果添加到进程输出中
        process.appendResult(result);
        // 结束命令处理
        process.end();
    }

}
